package controllers.admin.backup

import controllers.{ BaseAuthController, WebJarAssets }
import javax.inject.Inject
import models.annotation.AnnotationService
import models.document.DocumentService
import models.user.UserService
import models.user.Roles._
import play.api.Configuration
import scala.concurrent.{ ExecutionContext, Future }
  
class BackupAdminController @Inject() (
    val config: Configuration,
    val annotations: AnnotationService,
    val documents: DocumentService,
    val users: UserService,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarAssets
  ) extends BaseAuthController(config, documents, users) with RestoreAction {
  
  def index = StackAction(AuthorityKey -> Admin) { implicit request => 
    Ok(views.html.admin.backup.index())
  }
  
  def restoreDocument = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("backup-zip")) match {
      case Some(formData) =>
          restoreFromZip(formData.ref.file, annotations, documents).map(_ => Redirect(routes.BackupAdminController.index)) 
        
      case None => 
        Future.successful(BadRequest)
    }   
  }
  
  def restoreAnnotations = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("backup-jsonl")) match {
      case Some(formData) =>
        restoreFromJSONL(formData.ref.file, annotations).map(_ => Redirect(routes.BackupAdminController.index))
        
      case None =>
        Future.successful(BadRequest)
    }
  }
  
}