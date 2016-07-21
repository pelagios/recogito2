package controllers.admin.backup

import controllers.WebJarAssets
import controllers.BaseAuthController
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import scala.concurrent.{ ExecutionContext, Future }
import storage.DB
  
class BackupAdminController @Inject() (implicit val cache: CacheApi, val db: DB, ec: ExecutionContext, webjars: WebJarAssets) extends BaseAuthController with RestoreAction {
  
  def index = StackAction(AuthorityKey -> Admin) { implicit request => 
    Ok(views.html.admin.backup.index())
  }
  
  def restoreDocument = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("backup-zip")) match {
      case Some(formData) =>
          scala.concurrent.blocking {
            restoreFromZip(formData.ref.file).map(_ => Redirect(routes.BackupAdminController.index)) 
          }
        
      case None => 
        Future.successful(BadRequest)
    }   
  }
  
}