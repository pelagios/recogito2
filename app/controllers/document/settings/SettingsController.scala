package controllers.document.settings

import controllers.{ BaseAuthController, HasPrettyPrintJSON }
import controllers.document.settings.actions._
import javax.inject.{ Inject, Singleton }
import models.annotation.AnnotationService
import models.contribution.ContributionService
import models.document.{ DocumentService, DocumentInfo, DocumentAccessLevel }
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import models.user.UserService
import models.user.Roles._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.http.FileMimeTypes
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ AnyContent, Result, Request }
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json.{ Json, JsSuccess, JsError, Reads }
import scala.concurrent.{ Future, ExecutionContext }
import storage.Uploads

@Singleton
class SettingsController @Inject() (
    val config: Configuration,
    val users: UserService,
    val contributions: ContributionService,
    val documents: DocumentService,
    val annotations: AnnotationService,
    val uploads: Uploads,
    val messagesApi: MessagesApi,
    implicit val tmpFile: TemporaryFileCreator,
    implicit val mimeTypes: FileMimeTypes,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarsUtil
  ) extends BaseAuthController(config, documents, users)
      with MetadataActions
      with SharingActions
      with HistoryActions
      with BackupActions
      with DeleteActions
      with I18nSupport
      with HasPrettyPrintJSON {

  def showDocumentSettings(documentId: String, tab: Option[String]) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentAdminAction(documentId, loggedIn.username, { doc =>
      tab.map(_.toLowerCase) match {
        case Some(t) if t == "sharing" => {
          val f = for {
            collaborators <- documents.listDocumentCollaborators(documentId)
          } yield collaborators

          f.map(sharingPolicies =>
            // Make sure this page isn't cached, since stuff gets added via AJAX
            Ok(views.html.document.settings.sharing(doc, loggedIn, sharingPolicies))
              .withHeaders(
                CACHE_CONTROL -> "no-cache, no-store, must-revalidate",
                PRAGMA -> "no-cache",
                EXPIRES -> "0"))
        }

        case Some(t) if t == "history" =>
          Future.successful(Ok(views.html.document.settings.history(doc, loggedIn)))

        case Some(t) if t == "backup" =>
          Future.successful(Ok(views.html.document.settings.backup(doc, loggedIn)))

        case Some(t) if t == "delete" =>
          Future.successful(Ok(views.html.document.settings.delete(doc, loggedIn)))

        case _ =>
          Future.successful(Ok(views.html.document.settings.metadata(metadataForm(doc.document), doc, loggedIn)))
      }
    })
  }

  protected def documentAdminAction(
      documentId: String,
      username: String,
      action: DocumentInfo => Future[Result]
    ) = {

    documents.getExtendedInfo(documentId, Some(username)).flatMap(_ match {
      case Some((doc, accesslevel)) if (accesslevel.isAdmin) => action(doc)
      case Some(_) => Future.successful(ForbiddenPage)
      case None => Future.successful(NotFoundPage)
    })
  }

  protected def jsonDocumentAdminAction[T](
      documentId: String,
      username: String,
      action: (DocumentRecord, T) => Future[Result]
    )(implicit request: Request[AnyContent], reads: Reads[T]) = {

    request.body.asJson match {
      case Some(json) => Json.fromJson[T](json) match {
        case s: JsSuccess[T] =>
          documents.getDocumentRecord(documentId, Some(username)).flatMap(_ match {
            case Some((document, accesslevel))
              if (accesslevel == DocumentAccessLevel.OWNER || accesslevel == DocumentAccessLevel.ADMIN) =>
                action(document, s.get)

            case Some(_) => Future.successful(ForbiddenPage)
            case None => Future.successful(NotFoundPage)
          })

        case e: JsError =>
          Future.successful(BadRequest)
      }

      case None => Future.successful(BadRequest)
    }
  }

}
