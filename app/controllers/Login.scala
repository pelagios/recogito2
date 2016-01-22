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

case class LoginData(name: String, email: String, password: String)

/** Just temporary container for everything while we're still hacking on the basics **/
class Login @Inject() (implicit db: DB) extends Controller {

  val loginForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )

 def showLoginForm = Action {
    Ok(views.html.login(loginForm))
  }

  def processLogin = Action(parse.form(loginForm)) { implicit request =>
    // TODO error handling
    val loginData = request.body
    Logger.info(loginData.toString)
    Ok(views.html.landingPage())
  }

}
