package controllers.landing

import controllers.BaseController
import javax.inject.Inject
import jp.t2v.lab.play2.auth.Logout
import play.api.cache.CacheApi
import play.api.mvc.Action
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import storage.DB

class LogoutController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController with Logout {

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded
  }

}
