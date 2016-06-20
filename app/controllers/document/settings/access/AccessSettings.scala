package controllers.document.settings.access

import controllers.BaseController
import models.document._
import models.user.Roles._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

trait AccessSettings { self: BaseController =>
  
  def setIsPublic(docId: String, enabled: Boolean) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    DocumentService.findById(docId, Some(loggedIn.user.getUsername))(self.db).flatMap(_ match {
      case Some((document, accesslevel)) if accesslevel == DocumentAccessLevel.OWNER =>
        DocumentService.setPublicVisibility(document.getId, enabled)(self.db).map(_ => Status(200))
        
      case Some(_) =>
        Future.successful(Forbidden)
        
      case None =>
        Future.successful(NotFound)
    })
  }
  
}