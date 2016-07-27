package controllers.document.settings

import controllers.{ BaseAuthController, WebJarAssets }
import controllers.document.settings.actions._
import javax.inject.Inject
import models.annotation.AnnotationService
import models.contribution.ContributionService
import models.document.{ DocumentService, DocumentAccessLevel }
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import models.user.UserService
import models.user.Roles._
import play.api.Configuration
import play.api.mvc.{ AnyContent, Result, Request }
import play.api.libs.json.{ Json, JsSuccess, JsError, Reads }
import scala.concurrent.{ Future, ExecutionContext }
import storage.Uploads

class SettingsController @Inject() (
    val config: Configuration,
    val users: UserService,
    val contributions: ContributionService,
    val documents: DocumentService, 
    val annotations: AnnotationService,
    val uploads: Uploads,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarAssets
  ) extends BaseAuthController(config, documents, users)
      with MetadataActions with SharingActions with RollbackActions with BackupActions with DeleteActions {
    
  def showDocumentSettings(documentId: String, tab: Option[String]) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    // Settings page is only visible to OWNER or ADMIN access levels
    documentAdminAction(documentId, loggedIn.user.getUsername, { case (document, parts) =>
      tab.map(_.toLowerCase) match { 
        case Some(t) if t == "sharing" => {
          val f = for {
            collaborators <- documents.listDocumentCollaborators(documentId)
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
  
  protected def documentAdminAction(documentId: String, username: String, action: (DocumentRecord, Seq[DocumentFilepartRecord]) => Future[Result]) = {
    documents.findByIdWithFileparts(documentId, Some(username)).flatMap(_ match {      
      case Some((document, parts, accesslvl)) if (accesslvl.isAdmin) => action(document, parts)
      case Some(_) => Future.successful(ForbiddenPage)
      case None => Future.successful(NotFoundPage)
    })
  }
  
  protected def jsonDocumentAdminAction[T](documentId: String, username: String, action: (DocumentRecord, T) => Future[Result])(implicit request: Request[AnyContent], reads: Reads[T]) = {
    request.body.asJson match {
      case Some(json) => Json.fromJson[T](json) match {
        case s: JsSuccess[T] =>
          documents.findById(documentId, Some(username)).flatMap(_ match {
            case Some((document, accesslvl)) if (accesslvl == DocumentAccessLevel.OWNER || accesslvl == DocumentAccessLevel.ADMIN) => action(document, s.get)
            case Some(_) => Future.successful(ForbiddenPage)
            case None => Future.successful(NotFoundPage)
          })
        
        case e: JsError => Future.successful(BadRequest)
      }
        
      case None => Future.successful(BadRequest)
    }    
  }
  

}
