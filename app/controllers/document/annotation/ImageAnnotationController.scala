package controllers.document.annotation

import controllers.{ AbstractController, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.user.Roles._
import storage.DB

class ImageAnnotationController @Inject() (implicit val db: DB) extends AbstractController with AuthElement with Security {

  def showAnnotationViewForDocPart(documentId: String, partNo: Int) = StackAction(AuthorityKey -> Normal) { implicit request =>
    Ok("")
  }
  
}