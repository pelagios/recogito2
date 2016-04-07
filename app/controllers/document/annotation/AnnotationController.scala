package controllers.document.annotation

import controllers.BaseController
import javax.inject.Inject
import models.ContentType
import models.document.DocumentService
import models.user.Roles._
import play.api.Application
import play.api.cache.CacheApi
import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import storage.{ DB, FileAccess }
import play.api.Logger

class AnnotationController @Inject() (implicit val cache: CacheApi, val db: DB, val application: Application) extends BaseController with FileAccess {

  /** Just a redirect for convenience **/
  def showAnnotationViewForDoc(documentId: String) = StackAction(AuthorityKey -> Normal) { implicit request =>
    Redirect(routes.AnnotationController.showAnnotationViewForDocPart(documentId, 1))
  }

  def showAnnotationViewForDocPart(documentId: String, partNo: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    val username = loggedIn.user.getUsername  
  
    renderDocumentPartResponse(documentId, partNo, username, { case (document, parts, thisPart) =>
      ContentType.withName(thisPart.getContentType) match {
                    
        case ContentType.IMAGE_UPLOAD => 
          Ok(views.html.document.annotation.image(username, document, parts, thisPart))
        
        case ContentType.TEXT_PLAIN => {
          readTextfile(username, document.getId, thisPart.getFilename) match {
            case Some(content) =>
              Ok(views.html.document.annotation.text(username, document, parts, thisPart, content))

            case None => {
              // Filepart found in DB, but not file on filesystem
              Logger.error("Filepart recorded in the DB is missing on the filesystem: " + username + ", " + document.getId)
              InternalServerError
            }
          }
        }
      }
    })
  }

}