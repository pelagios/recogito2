package controllers.my.ng.documents

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, ControllerComponents, Request, Result}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import services.annotation.AnnotationService
import services.contribution.ContributionService
import services.document.DocumentService
import services.user.UserService

@Singleton
class DocumentInfoController @Inject() (
    val annotations: AnnotationService,
    val components: ControllerComponents,
    val contributions: ContributionService,
    val documents: DocumentService,
    val silhouette: Silhouette[Security.Env],
    val users: UserService,
    val config: Configuration,
    implicit val ctx: ExecutionContext
  ) extends BaseController(components, config, users)
      with SortByDB
      with SortByIndex
      with HasPrettyPrintJSON {

  // Document properties derived from the index
  private val INDEX_SORT_PROPERTIES = 
    Seq("last_edit_at", "last_edit_by", "annotations")

  private def isSortingByIndex(sortBy: String) =
    INDEX_SORT_PROPERTIES.contains(sortBy.toLowerCase)

  /** Takes a list of document IDs and, for each, fetches last edit and number of annotations from the index **/
  protected def fetchIndexProperties(docIds: Seq[String], config: PresentationConfig) = {
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

  /** Common boilerplate code **/
  private def getDocumentInfo(
    username: String, offset: Int, size: Int,
    onSortByDB   : (String, Int, Int, Option[PresentationConfig]) => Future[Result],
    onSortByIndex: (String, Int, Int, PresentationConfig) => Future[Result]
  )(implicit request: Request[AnyContent]) = {
    val config = request.body.asJson.flatMap(json => 
      Try(Json.fromJson[PresentationConfig](json).get).toOption)

    // Does the config request a sorting based on an index property?
    val sortByIndex = config
      .flatMap(_.sort.map(_.sortBy))
      .map(isSortingByIndex(_))
      .getOrElse(false)

    if (sortByIndex)
      onSortByIndex(username, offset, size, config.get)
    else 
      onSortByDB(username, offset, size, config)
  }

  /** Returns the list of my documents, taking into account col/sort config **/
  def getMyDocuments(offset: Int, size: Int) = silhouette.SecuredAction.async { implicit request =>
    getDocumentInfo(
      request.identity.username, 
      offset, 
      size, 
      getMyDocumentsSortedByDB, 
      getMyDocumentsSortedByIndex)
  }

  /** Returns the list of documents shared with me, taking into account col/sort config **/
  def getSharedWithMe(offset: Int, size: Int) = silhouette.SecuredAction.async { implicit request =>
    getDocumentInfo(
      request.identity.username, 
      offset, 
      size, 
      getSharedDocumentsSortedByDB, 
      getSharedDocumentsSortedByIndex)
  }

  def getAccessibleDocuments(offset: Int, size: Int) = silhouette.UserAwareAction.async { implicit request =>
    ???
  }

}
