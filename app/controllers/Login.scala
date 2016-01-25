package controllers

import database.DB
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.{ Action, Controller }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data.Form
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import jp.t2v.lab.play2.auth.LoginLogout
import scala.concurrent.Future

case class LoginData(name: String, password: String)

/** Just temporary container for everything while we're still hacking on the basics **/
class Login @Inject() (implicit val db: DB) extends Controller with LoginLogout with HasDB with AuthConfigImpl {

  val loginForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )

  def showLoginForm = Action {
    Ok(views.html.login(loginForm))
  }

  def processLogin = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        Logger.info("Bad request!")
        Future.successful(BadRequest(views.html.login(formWithErrors)))
      },

      loginData => {
        Logger.info(loginData.name + ", " + loginData.password)
        gotoLoginSucceeded(loginData.name)
        // Redirect(routes.Application.landingPage()) // .flashing("success" -> "Contact saved!")
      }
    )
  }

}
