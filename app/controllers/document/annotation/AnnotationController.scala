package controllers.document.annotation

import controllers.{ AbstractController, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.content.{ ContentType, DocumentService }
import models.user.Roles._
import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import storage.{ DB, FileAccess }

class AnnotationController @Inject() (implicit val db: DB) extends AbstractController with AuthElement with Security with FileAccess {

  /** Just a redirect for convenience **/
  def showAnnotationViewForDoc(documentId: String) = StackAction(AuthorityKey -> Normal) { implicit request =>
    Redirect(routes.AnnotationController.showAnnotationViewForDocPart(documentId, 1))
  }

  def showAnnotationViewForDocPart(documentId: String, partNo: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    val username = loggedIn.getUsername

    DocumentService.findByIdWithFileparts(documentId).map(_ match {
      case Some((document, fileparts)) => {
        // Verify if the user is allowed to access this document - TODO what about shared content?
        if (document.getOwner == username) {
          fileparts.find(_.getSequenceNo == partNo) match {
            case Some(filepart) => ContentType.withName(filepart.getContentType) match {
              
              case ContentType.IMAGE_UPLOAD => 
                Ok(views.html.document.annotation.image(username, document, fileparts, filepart))
                
              case ContentType.TEXT_PLAIN => {
                loadTextfile(username, filepart.getFilename) match {
                  case Some(content) =>
                    Ok(views.html.document.annotation.text(username, document, fileparts, filepart, content))

                  case None => {
                    // Filepart found in DB, but not file on filesystem
                    InternalServerError
                  }
                }
              }
            }

            case None =>
              // No filepart with the specified sequence no
              NotFound
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

}