package controllers.my.duplicate

import controllers.{Security, HasPrettyPrintJSON}
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.AnnotationService
import services.document.DocumentService

@Singleton
class DuplicateController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[Security.Env], 
  annotationService: AnnotationService,
  documentService: DocumentService,
  implicit val ctx: ExecutionContext
) extends AbstractController(components) with HasPrettyPrintJSON {
  
  /** API method to create a copy of a document in the workspace **/
  def duplicateDocument(id: String) = silhouette.SecuredAction.async { implicit request => 
    documentService.getExtendedMeta(id: String, Some(request.identity.username)).flatMap { _ match { 
      case Some((doc, accesslevel)) =>
        if (accesslevel.isAdmin) { // For the time being, enforce admin access         
          val f = for {
            cloned <- documentService.duplicateDocument(doc.document, doc.fileparts)
            success <- annotationService.cloneAnnotationsTo(
              cloned.docIdBefore,
              cloned.docIdAfter,
              cloned.filepartIds)
            _ <- Future { Thread.sleep(1000) } // Horrible but ES needs time to reflect the update
          } yield (success)

          f.map { success => 
            if (success) Ok else InternalServerError
          }
        } else {
          Future.successful(Forbidden)
        } 

      case None => Future.successful(NotFound)
    }}
  }

}
