package controllers.document.annotation

import controllers.WebJarAssets
import controllers.{ HasCache, HasDatabase, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.OptionalAuthElement
import models.ContentType
import models.document.{ DocumentAccessLevel, DocumentService }
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import play.api.cache.CacheApi
import play.api.mvc.{ Controller, RequestHeader }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import storage.{ DB, FileAccess }

class AnnotationController @Inject() (implicit val cache: CacheApi, val db: DB, webjars: WebJarAssets) 
  extends Controller with HasCache with HasDatabase with OptionalAuthElement with Security with FileAccess {

  /** Just a redirect for convenience **/
  def showAnnotationViewForDoc(documentId: String) = StackAction { implicit request =>
    Redirect(routes.AnnotationController.showAnnotationViewForDocPart(documentId, 1))
  }

  def showAnnotationViewForDocPart(documentId: String, partNo: Int) = AsyncStack { implicit request =>
    val username = loggedIn.map(_.user.getUsername)
    DocumentService.findByIdWithFileparts(documentId, username).map(_ match {

      case Some((document, fileparts, accesslevel)) =>
        if (accesslevel.canRead) {
          val selectedPart = fileparts.filter(_.getSequenceNo == partNo)
          if (selectedPart.size == 1)
            renderResponse(username, document, fileparts, selectedPart.head, accesslevel)
          else if (selectedPart.isEmpty)
            NotFound
          else
            // More than one part with this sequence number - DB integrity broken!
            throw new Exception("Invalid document part")
        } else {
          Forbidden
        }

      // No document with that ID found in DB
      case None => NotFound
    })
  }

  private def renderResponse(loggedInUser: Option[String], document: DocumentRecord, parts: Seq[DocumentFilepartRecord],
      thisPart: DocumentFilepartRecord, accesslevel: DocumentAccessLevel)(implicit request: RequestHeader) =

    ContentType.withName(thisPart.getContentType) match {

      case Some(ContentType.IMAGE_UPLOAD) =>
        Ok(views.html.document.annotation.image(loggedInUser, document, parts, thisPart, accesslevel))

      case Some(ContentType.TEXT_PLAIN) => {
        readTextfile(document.getOwner, document.getId, thisPart.getFilename) match {
          case Some(content) =>
            Ok(views.html.document.annotation.text(loggedInUser, document, parts, thisPart, accesslevel, content))

          case None => {
            // Filepart found in DB, but not file on filesystem
            Logger.error("Filepart recorded in the DB is missing on the filesystem: " + document.getOwner + ", " + document.getId)
            InternalServerError
          }
        }
      }

      case _ =>
        // Unknown content type in DB, or content type we don't have an annotation view for - should never happen
        InternalServerError
    }

}
