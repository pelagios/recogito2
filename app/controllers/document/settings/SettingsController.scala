package controllers.document.settings

import controllers.{ BaseAuthController, WebJarAssets }
import controllers.document.settings.actions._
import javax.inject.Inject
import models.document.DocumentService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import models.user.Roles._
import play.api.cache.CacheApi
import play.api.mvc.{ AnyContent, Result, Request }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import storage.DB
import play.api.libs.json.{ Json, JsSuccess, JsError, Reads }

/** Holds the boilerplate needed to verify user access level is OWNER or ADMIN **/
trait HasAdminAction { self: BaseAuthController =>
  
  import models.document.DocumentAccessLevel._
  
  def documentAdminAction(documentId: String, username: String, action: (DocumentRecord, Seq[DocumentFilepartRecord]) => Future[Result]) = {
    DocumentService.findByIdWithFileparts(documentId, Some(username))(self.db).flatMap(_ match {      
      case Some((document, parts, accesslvl)) if (accesslvl.isAdmin) => action(document, parts)
      case Some(_) => Future.successful(ForbiddenPage)
      case None => Future.successful(NotFoundPage)
    })
  }
  
  def jsonDocumentAdminAction[T](documentId: String, username: String, action: (DocumentRecord, T) => Future[Result])(implicit request: Request[AnyContent], reads: Reads[T]) = {
    request.body.asJson match {
      case Some(json) => Json.fromJson[T](json) match {
        case s: JsSuccess[T] =>
          DocumentService.findById(documentId, Some(username))(self.db).flatMap(_ match {
            case Some((document, accesslvl)) if (accesslvl == OWNER || accesslvl == ADMIN) => action(document, s.get)
            case Some(_) => Future.successful(ForbiddenPage)
            case None => Future.successful(NotFoundPage)
          })
        
        case e: JsError => Future.successful(BadRequest)
      }
        
      case None => Future.successful(BadRequest)
    }    
  }
  
}

class SettingsController @Inject() (implicit val cache: CacheApi, val db: DB, webjars: WebJarAssets)
  extends BaseAuthController
    with HasAdminAction
    with MetadataActions
    with SharingActions
    with RollbackActions
    with BackupActions
    with DeleteActions {
    
  def showDocumentSettings(documentId: String, tab: Option[String]) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    // Settings page is only visible to OWNER or ADMIN access levels
    documentAdminAction(documentId, loggedIn.user.getUsername, { case (document, parts) =>
      tab.map(_.toLowerCase) match { 
        case Some(t) if t == "sharing" => {
          val f = for {
            collaborators <- DocumentService.listDocumentCollaborators(documentId)
          } yield collaborators
           
          f.map(t => 
            // Make sure this page isn't cached, since stuff gets added via AJAX
            Ok(views.html.document.settings.sharing(loggedIn.user.getUsername, document, t))
              .withHeaders(
                CACHE_CONTROL -> "no-cache, no-store, must-revalidate",
                PRAGMA -> "no-cache",
                EXPIRES -> "0"))
        }
            
        case Some(t) if t == "history" =>
          Future.successful(Ok(views.html.document.settings.history(loggedIn.user.getUsername, document)))
          
        case Some(t) if t == "backup" =>
          Future.successful(Ok(views.html.document.settings.backup(loggedIn.user.getUsername, document)))
          
        case _ =>
          Future.successful(Ok(views.html.document.settings.metadata(loggedIn.user.getUsername, document))) 
      }
    })
  }

}
