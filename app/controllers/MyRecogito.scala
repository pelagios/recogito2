package controllers

import database.DB
import models.Roles._
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import play.api.mvc.{ Action, Controller }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class MyRecogito @Inject() (implicit val db: DB) extends Controller with HasDB with AuthElement with AuthConfigImpl {
  
   def index = StackAction(AuthorityKey -> Normal) { implicit request =>
     Ok(views.html.myrecogito(loggedIn))
  }
   
}