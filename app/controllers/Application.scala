package controllers

import db.DB
import models.User
import play.api._
import play.api.mvc._
import models.User
import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.collection.JavaConversions._


class Application @Inject() (db: DB) extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def listAllUsers = Action.async {
    val users = new User(db).listAll()
    users.map { u =>  
      Ok(views.html.users(u.toList))
    }
  }

  
}
