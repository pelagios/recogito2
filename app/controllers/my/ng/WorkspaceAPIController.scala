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

  /** Deletes the document with the given ID, along with all annotations and files **/
  def deleteDocument(docId: String) = silhouette.SecuredAction.async { implicit request =>
    getIfOwner(docId, request.identity.username).flatMap { _ match {
      case Some(document) => 
        deleteOneDocument(document).map { success =>
          if (success) Ok
          else InternalServerError
        }
      case None => Future.successful(Forbidden)
    }}
  }

  /** Deletes the user's sharing policy for the given document */
  def unshareDocument(docId: String) = silhouette.SecuredAction.async { implicit request =>
    documents.removeDocumentCollaborator(docId, request.identity.username).map { success =>
      if (success) Ok else BadRequest
    }
  }

  private def getDocIdPayload(payload: Option[JsValue]) = payload match {
    case Some(json) => 
      Try(Json.fromJson[Seq[String]](json).get)
        .toOption.getOrElse(Seq.empty[String])

    case None => Seq.empty[String]
  }

  /** Bulk version of deleteDocument **/
  def bulkDeleteDocuments() = silhouette.SecuredAction.async { implicit request => 
    val docIds = getDocIdPayload(request.body.asJson)

    // All documents this user can - and is allowed to - delete
    val fDeleteableDocuments = Future.sequence {
      docIds.map(getIfOwner(_, request.identity.username))
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