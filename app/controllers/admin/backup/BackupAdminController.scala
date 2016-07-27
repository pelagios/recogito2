package controllers.admin.backup

import controllers.{ BaseAuthController, ControllerContext }
import javax.inject.Inject
import models.user.Roles._
import scala.concurrent.Future
import models.annotation.AnnotationService
  
class BackupAdminController @Inject() (annotationService: AnnotationService, implicit val ctx: ControllerContext) extends BaseAuthController with RestoreAction {
  
  def index = StackAction(AuthorityKey -> Admin) { implicit request => 
    Ok(views.html.admin.backup.index())
  }
  
  def restoreDocument = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("backup-zip")) match {
      case Some(formData) =>
          scala.concurrent.blocking {
            restoreFromZip(formData.ref.file, annotationService).map(_ => Redirect(routes.BackupAdminController.index)) 
          }
        
      case None => 
        Future.successful(BadRequest)
    }   
  }
  
}