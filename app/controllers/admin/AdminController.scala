package controllers.admin

import controllers.{ AbstractController, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.user.Roles._
import storage.DB

class AdminController @Inject() (implicit val db: DB) extends AbstractController with AuthElement with Security {
  
  def index = StackAction(AuthorityKey -> Admin) { implicit request =>
    Ok(views.html.admin.index())
  }
  
}