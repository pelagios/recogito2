package controllers.my.directory.delete

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.ControllerComponents
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import services.annotation.AnnotationService
import services.contribution.ContributionService
import services.document.{DocumentService, RuntimeAccessLevel}
import services.folder.FolderService
import services.generated.tables.records.DocumentRecord
import services.user.UserService

@Singleton
class DocumentActionsController @Inject() (
  val annotations: AnnotationService,
  val components: ControllerComponents,
  val contributions: ContributionService,
  val documents: DocumentService,
  val folders: FolderService,
  val silhouette: Silhouette[Security.Env],
  val users: UserService,
  val config: Configuration,
  implicit val ctx: ExecutionContext
) extends BaseController(components, config, users)
    with HasPrettyPrintJSON {

  /** Helper to get the a list of doc IDs from the JSON payload */
  protected def getDocIdPayload(payload: Option[JsValue]) = payload match {
    case Some(json) => 
      Try(Json.fromJson[Seq[String]](json).get)
        .toOption.getOrElse(Seq.empty[String])

    case None => Seq.empty[String]
  }

  /** Returns the document record from the DB, but only if the given user is the owner.
    * 
    * Will return none in error cases as well, i.e. when the document wasn't found, or something
    * went wrong.
    */
  private def getDocumentIfOwned(docId: String, username: String): Future[Option[DocumentRecord]] =
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

  def deleteDocument(docId: String) = silhouette.SecuredAction.async { implicit request => 
    getDocumentIfOwned(docId, request.identity.username).flatMap { _ match {
      case Some(document) => 
        deleteOneDocument(document).map { success =>
          if (success) Ok
          else InternalServerError
        }
      case None => Future.successful(Forbidden)
    }}
  }

  def bulkDeleteDocuments() = silhouette.SecuredAction.async { implicit request =>
    val docIds = getDocIdPayload(request.body.asJson)

    // All documents this user can - and is allowed to - delete
    val fDeleteableDocuments = Future.sequence {
      docIds.map(getDocumentIfOwned(_, request.identity.username))
    } map { _.flatten }

    val fSuccess = fDeleteableDocuments.flatMap { toDelete =>
      Future.sequence(toDelete.map(deleteOneDocument))
    }

    fSuccess.map { results => 
      // Number of actual delete attempt = num. of allowed documents
      val numDeleted = results.size 
      val success = !results.exists(!_) // No "false" exists in the list

      if (success && numDeleted == docIds.size) 
        Ok 
      else if (numDeleted != docIds.size) 
        Forbidden // Not ideal response, since some docs might have been deleted
      else
        InternalServerError
    }
  }

  /** Deletes the user's sharing policy for the given document */
  def unshareDocument(docId: String) = silhouette.SecuredAction.async { implicit request =>
    documents.removeDocumentCollaborator(docId, request.identity.username).map { success =>
      if (success) Ok else BadRequest
    }
  }

  /** Bulk version of unshareDocument **/
  def bulkUnshareDocuments() = silhouette.SecuredAction.async { implicit request => 
    val docIds = getDocIdPayload(request.body.asJson)
    val user = request.identity.username

    Future.sequence {
      docIds.map(documents.removeDocumentCollaborator(_, user))
    }.map { results =>
      val success = !results.exists(!_)
      if (success) Ok else BadRequest
    }
  }

}