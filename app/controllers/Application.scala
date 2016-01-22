package controllers

import database.DB
import models.Users
import javax.inject.Inject
import play.api.mvc.{ Action, Controller }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class Application @Inject() (implicit db: DB) extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def listUsers = Action.async {
    val users = Users.listAll()
    users.map(u => Ok(views.html.users(u)))
  }

}
