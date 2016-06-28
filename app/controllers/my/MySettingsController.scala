package controllers.my

import controllers.{ BaseController, WebJarAssets }
import javax.inject.Inject
import play.api.cache.CacheApi
import storage.DB

class UploadController @Inject() (implicit val cache: CacheApi, val db: DB, webjars: WebJarAssets) extends BaseController {
    
  def accountSettings() = StackAction(AuthorityKey -> Normal) { implicit request =>
    Ok("TODO")
  }
  
}


 
