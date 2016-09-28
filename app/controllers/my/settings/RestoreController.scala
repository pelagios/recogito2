package controllers.my.settings

import controllers.{ HasUserService, HasConfig, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.user.Roles._
import models.user.UserService
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.Controller
import play.api.Configuration

class RestoreController @Inject() (
    val config: Configuration,
    val users: UserService,
    val messagesApi: MessagesApi
) extends Controller with AuthElement with HasUserService with HasConfig with Security with I18nSupport {

  def index() = StackAction(AuthorityKey -> Normal) { implicit request =>
    Ok(views.html.my.settings.restore(loggedIn.user))
  }

}
