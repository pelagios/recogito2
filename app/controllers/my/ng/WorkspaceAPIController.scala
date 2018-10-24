package controllers.my.ng

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import services.{HasDate, Page, SortOrder}
import services.annotation.AnnotationService
import services.contribution.{Contribution, ContributionService}
import services.document.{DocumentService, RuntimeAccessLevel}
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.user.UserService
import storage.uploads.Uploads

/** A quick hack for local testing of the new React UI **/
@Singleton
class WorkspaceAPIController @Inject() (
    val components: ControllerComponents,
    val annotations: AnnotationService,
    val contributions: ContributionService,
    val documents: DocumentService,
    val silhouette: Silhouette[Security.Env],
    val users: UserService,
    val uploads: Uploads,
    val config: Configuration,
    implicit val ctx: ExecutionContext
  ) extends BaseController(components, config, users)
      with HasPrettyPrintJSON 
      with HasDate {
  
  private val INDEX_SORT_PROPERTIES = Seq("last_edit_at", "last_edit_by", "annotations")
  
  private def isSortingByIndex(sortBy: String) =
    INDEX_SORT_PROPERTIES.contains(sortBy.toLowerCase)

  /** Utility to get the document, but only if the given user is the document's owner
    * 
    * Will return none in error cases as well, i.e. when the document wasn't found, or something
    * went wrong.
    */
  private def getIfOwner(docId: String, username: String): Future[Option[DocumentRecord]] =
    documents.getDocumentRecord(docId, Some(username)).map(_ match {
      case Some((document, accesslevel)) =>
        if (accesslevel == RuntimeAccessLevel.OWNER) Some(document)
        else None
      case None => None
    }).recover { case t =>
      None
    }

  /** Deletes one document. 
    * 
    * WARNING: this method DOES NOT CHECK ACCESS PERMISSONS. Ensure that whoever triggered 
    * it is allowed to delete.
    */
  private def deleteOneDocument(doc: DocumentRecord): Future[Boolean] = {
    val deleteDocument = documents.delete(doc)
    val deleteAnnotations = annotations.deleteByDocId(doc.getId)
    val deleteContributions = contributions.deleteHistory(doc.getId) 
    for {
      _ <- documents.delete(doc)
      s1 <- deleteAnnotations
      s2 <- deleteContributions
    } yield (s1 && s2)
  }

  /** Returns account information **/
  def account = silhouette.SecuredAction.async { implicit request =>
    val username = request.identity.username

    val fUser = users.findByUsername(username)
    val fMyDocCount = documents.countByOwner(username)
    val fSharedCount = documents.countBySharedWith(username)

    val f = for {
      user <- fUser
      myDocCount <- fMyDocCount
      sharedCount <- fSharedCount
    } yield (user.get, myDocCount, sharedCount)

    f.map { case (user, myDocCount, sharedCount) =>
      val usedMb = Math.round(100 * uploads.getUsedDiskspaceKB(username) / 1024).toDouble / 100
      jsonOk(Json.obj(
        "username" -> user.username,
        "real_name" -> user.realName,
        "member_since" -> formatDate(new DateTime(user.memberSince.getTime)),
        "documents" -> Json.obj(
          "my_documents" -> myDocCount,
          "shared_with_me" -> sharedCount
        ),
        "storage" -> Json.obj(
          "quota_mb" -> user.quotaMb.toInt,
          "used_mb" -> usedMb
        )
      ))
    }
  }

  /** Takes a list of document IDs and, for each, fetches last edit and number of annotations from the index **/
  private def fetchIndexProperties(docIds: Seq[String], config: PresentationConfig) = {
    // Helper that wraps the common bits: conditional execution, sequence-ing, mapping to (id -> result) tuple
    def fetchIfRequested[T](field: String*)(fn: String => Future[T]) =
      if (config.hasAnyColumn(field))
        Future.sequence(docIds.map(id => fn(id).map((id, _)))) 
      else 
        Future.successful(Seq())

    val fLastEdits = fetchIfRequested("last_edit_at", "last_edit_by") { id =>
      contributions.getLastContribution(id)
    }

    val fAnnotationCount = fetchIfRequested("annotations") { id =>
      annotations.countByDocId(id)
    }

    // TODO status_ratio, activity
          
    val f = for {
      lastEdits <- fLastEdits
      annotationCounts <- fAnnotationCount
    } yield (lastEdits.toMap, annotationCounts.toMap)   
    
    f.map { case (lastEdits, annotationsPerDoc) =>
      docIds.map { id =>
        val lastEdit = lastEdits.find(_._1 == id).flatMap(_._2)
        val annotations = annotationsPerDoc.find(_._1 == id).map(_._2).getOrElse(0l)
        (id, IndexDerivedProperties(lastEdit.map(_.madeAt), lastEdit.map(_.madeBy), Some(annotations)))
      }
    }
  }

  private def sortByIndexProperty(
    docIds: Seq[String], 
    sort: Sorting,
    offset: Int, 
    size: Int
  ): Future[Seq[String]] = sort.sortBy match {
    case "last_edit_at" => contributions.sortDocsByLastModifiedAt(docIds, sort.order, offset, size)
    case "last_edit_by" => contributions.sortDocsByLastModifiedBy(docIds, sort.order, offset, size)
    case "annotations" => annotations.sortDocsByAnnotationCount(docIds, sort.order, offset, size)
    case _ => Future.successful(docIds)
  }
  
  /** Boilerplate code to fetch documents sorted via a DB property */
  private def documentsByDB[T <: Product](
    username: String,
    offset: Int, 
    size: Int, 
    config: Option[PresentationConfig],
    fn: (String, Int, Int, Option[String], Option[SortOrder]) => Future[Page[T]]
  ) = for {
    documents <- fn(username, offset, size, 
      config.flatMap(_.sort.map(_.sortBy)),
      config.flatMap(_.sort.map(_.order)))

    indexProperties <- config match {
      case Some(c) => 
        val ids = documents.items.map(_.productElement(0).asInstanceOf[DocumentRecord].getId)
        fetchIndexProperties(ids, c).map(Some(_))

      case None => Future.successful(None)
    }
  } yield (documents, indexProperties)

  /** Fetches 'Shared with me' documents sorted by DB property **/
  private def sharedDocumentsByDB(
    username: String,
    offset: Int, 
    size: Int, 
    config: Option[PresentationConfig]
  )(implicit request: Request[AnyContent]) = {   
    documentsByDB(
      username, offset, size, config, documents.findSharedWithPart
    ).map { case (documents, indexProperties) =>
      val interleaved = ConfiguredPresentation.forSharedDocument(documents, indexProperties.map(_.toMap), config.map(_.columns))
      jsonOk(Json.toJson(interleaved))
    }
  }

  /** Fetches 'My Documents' sorted by DB property **/
  private def myDocumentsByDB(
    username: String,
    offset: Int, 
    size: Int, 
    config: Option[PresentationConfig]
  )(implicit request: Request[AnyContent]) = {
    documentsByDB(
      username, offset, size, config, documents.findByOwnerWithParts
    ).map { case (documents, indexProperties) =>
      val interleaved = ConfiguredPresentation.forMyDocument(documents, indexProperties.map(_.toMap), config.map(_.columns))
      jsonOk(Json.toJson(interleaved))
    }
  }

  private def myDocumentsByIndex(
    username: String,
    offset: Int, 
    size: Int,
    config: PresentationConfig
  )(implicit request: Request[AnyContent]) = {
    val startTime = System.currentTimeMillis

    val f = for {
      allIds <- documents.listAllIdsByOwner(username)
      sortedIds <- sortByIndexProperty(allIds, config.sort.get, offset, size)
      docsWithParts <- documents.findByIdsWithParts(sortedIds)
      indexProperties <- fetchIndexProperties(sortedIds, config)      
    } yield (allIds, sortedIds, docsWithParts, indexProperties)

    f.map { case (allIds, sortedIds, docsWithParts, indexProperties) => 
      val dbResult = Page(System.currentTimeMillis - startTime, sortedIds.size, offset, size, docsWithParts)
      val interleaved = ConfiguredPresentation.forMyDocument(dbResult, Some(indexProperties.toMap), Some(config.columns))
      jsonOk(Json.toJson(interleaved))
    }
  }

  private def sharedDocumentsByIndex(
    username: String,
    offset: Int, 
    size: Int,
    config: PresentationConfig
  )(implicit request: Request[AnyContent]) = {
    val startTime = System.currentTimeMillis

    val f = for {
      allIds <- documents.listAllIdsSharedWith(username)
      sortedIds <- sortByIndexProperty(allIds, config.sort.get, offset, size)
      documents <- documents.findByIdsWithPartsAndSharingPolicy(sortedIds, username)
      indexProperties <- fetchIndexProperties(sortedIds, config)
    } yield (allIds, sortedIds, documents, indexProperties)

    f.map { case (allIds, sortedIds, documents, indexProperties) =>
      val dbResult = Page(System.currentTimeMillis - startTime, sortedIds.size, offset, size, documents)
      val interleaved = ConfiguredPresentation.forSharedDocument(dbResult, Some(indexProperties.toMap), Some(config.columns))
      jsonOk(Json.toJson(interleaved))
    }
  }

  // Helper to capture common bits for /my-documents and /shared-with-me requests
  private def documentAPIRequest(
    username: String, offset: Int, size: Int,
    onSortByDB   : (String, Int, Int, Option[PresentationConfig]) => Future[Result],
    onSortByIndex: (String, Int, Int, PresentationConfig) => Future[Result]
  )(implicit request: Request[AnyContent]) = {
    val config = request.body.asJson.flatMap(json => 
      Try(Json.fromJson[PresentationConfig](json).get).toOption)

    // Does the config request a sorting based on an index property?
    val sortByIndex = config.flatMap(_.sort.map(_.sortBy)).map(isSortingByIndex(_)).getOrElse(false)
    if (sortByIndex)
      onSortByIndex(username, offset, size, config.get)
    else 
      onSortByDB(username, offset, size, config)
  }

  /** Returns the list of my documents, taking into account user-specified col/sort config **/
  def myDocuments(offset: Int, size: Int) = silhouette.SecuredAction.async { implicit request =>
    documentAPIRequest(
      request.identity.username, 
      offset, 
      size, 
      myDocumentsByDB, 
      myDocumentsByIndex)
  }
  
  /** Returns the list of documents shared with me, taking into account user-specified col/sort config **/
  def sharedWithMe(offset: Int, size: Int) = silhouette.SecuredAction.async { implicit request =>
    documentAPIRequest(
      request.identity.username, 
      offset, 
      size, 
      sharedDocumentsByDB, 
      sharedDocumentsByIndex)
  }

  /** Deletes the document with the given ID, along with all annotations and files **/
  def deleteDocument(docId: String) = silhouette.SecuredAction.async { implicit request =>
    getIfOwner(docId, request.identity.username).flatMap { _ match {
      case Some(document) => 
        deleteOneDocument(document).map { success =>
          if (success) Ok
          else InternalServerError
        }
      case None => Future.successful(BadRequest)
    }}
  }

  def bulkDeleteDocuments() = silhouette.SecuredAction.async { implicit request => 
    val docIds = request.body.asJson match {
      case Some(json) => 
        Try(Json.fromJson[Seq[String]](json).get)
          .toOption.getOrElse(Seq.empty[String])

      case None => Seq.empty[String]
    }

    // All documents this user can - and is allowed to - delete
    val fDeleteableDocuments = Future.sequence {
      docIds.map(getIfOwner(_, request.identity.username))
    } map { _.flatten }

    val fSuccess = fDeleteableDocuments.flatMap { toDelete =>
      Future.sequence(toDelete.map(deleteOneDocument))
    } map { !_.exists(!_) } // "No false exists in the list"

    fSuccess.map { success => 
      if (success) Ok else InternalServerError
    }
  }

}