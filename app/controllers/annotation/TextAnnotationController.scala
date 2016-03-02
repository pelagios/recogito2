package controllers.annotation

import controllers.{ AbstractController, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.Roles._
import models.{ AnnotationService, DocumentService }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import storage.{ DB, FileAccess }
import scala.concurrent.Future
import play.api.mvc.Action
import play.api.libs.json.Json

class TextAnnotationController @Inject() (implicit val db: DB) extends AbstractController with AuthElement with Security with FileAccess {
    
  /** TODO temporary dummy implementation **/
  def showTextAnnotationView(documentId: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    val username = loggedIn.getUsername
    
    DocumentService.findByIdWithFileparts(documentId).map(_ match {
      case Some((document, fileparts)) => {
        // Verify if the user is allowed to access this document - TODO what about shared content?
        if (document.getOwner == username) {
          loadTextfile(username, fileparts.head.getFilename) match {
            case Some(content) =>
              Ok(views.html.annotation.text_annotation(document.getId, fileparts.head.getId, content))
            
            case None => {
              // Filepart found in DB, but no file on filesystem
              InternalServerError
            }
          }
        } else {
          Forbidden
        }
      }
        
      case None =>
        // No document with that ID found in DB
        NotFound
    })
  }
  
  def getAnnotationsFor(filepartId: Int) = Action.async { request =>
    AnnotationService.findByFilepartId(filepartId)
      .map(annotations => Ok(Json.toJson(annotations)))
  }
  
}