package controllers

import database.DB
import models.Users
import javax.inject.Inject
import play.api.mvc.{ Action, Controller }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/** Just temporary container for everything while we're still hacking on the basics **/
class Application @Inject() (implicit db: DB) extends Controller {

  def landingPage = Action {
    Ok(views.html.landingPage())
  }

  def login = Action {
    Ok(views.html.login())
  }

  def createAccount = Action {
    Ok(views.html.signUp())
  }

  def listUsers = Action.async {
    val users = Users.listAll()
    users.map(u => Ok(views.html.users(u)))
  }

}
