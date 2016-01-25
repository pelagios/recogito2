package controllers

import database.DB
import models.Users
import models.Roles._
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import play.api.mvc.{ Action, Controller }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/** Just temporary container for everything while we're still hacking on the basics **/
class Application @Inject() (implicit val db: DB) extends Controller with HasDB with AuthElement with AuthConfigImpl {

  def landingPage = Action {
    Ok(views.html.landingPage())
  }

  def listUsers = Action.async {
    val users = Users.listAll()
    users.map(u => Ok(views.html.users(u)))
  }
  
}
