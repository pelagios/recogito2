package controllers.my.ng

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{Action, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import services.HasDate
import services.annotation.AnnotationService
import services.contribution.ContributionService
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
  
  // DocumentRecord JSON serialization     
  import services.document.DocumentService.documentRecordWrites
    
  implicit val documentWithMetadataWrites: Writes[(DocumentRecord, Seq[DocumentFilepartRecord])] = (
    (JsPath).write[DocumentRecord] and
    (JsPath \ "filetypes").write[Seq[String]] and
    (JsPath \ "file_count").write[Int]
  )(t => (
    t._1,
    t._2.map(_.getContentType).distinct,
    t._2.size
  ))

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
  
  /** Returns the list of my documents, taking into account user-specified col/sort config **/
  def myDocuments(offset: Int, size: Int) = silhouette.SecuredAction.async { implicit request =>
    val config = request.body.asJson.flatMap { json => 
      Try(Json.fromJson[PresentationConfig](json).get).toOption
    }

    // play.api.Logger.info(config.toString)

    documents.findByOwnerWithPartMetadata(request.identity.username, offset, size).map { result =>
      jsonOk(Json.toJson(result))
    }
  }
  
  /** Returns the list of documents shared with me, taking into account user-specified col/sort config **/
  def sharedWithMe(offset: Int, size: Int) = silhouette.SecuredAction.async { implicit request =>
    documents.findBySharedWith("username", offset, size).map { documents =>
      // TODO hack for testing only!
      jsonOk(Json.toJson(documents.items.map(_._1)))
    }
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