package controllers.document.settings

import controllers.BaseController
import javax.inject.Inject
import models.contribution.ContributionService
import models.generated.tables.records.DocumentRecord
import models.user.Roles._
import play.api.cache.CacheApi
import storage.DB

class SettingsController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController {
  
  def showDocumentSettings(documentId: String, tab: Option[String]) = AsyncStack(AuthorityKey -> Normal) { implicit request =>    
    renderDocumentResponse(documentId, loggedIn.user.getUsername,
      { case (document, fileparts) =>
        tab.map(_.toLowerCase) match {
          case Some(t) if t == "sharing" =>
            Ok(views.html.document.settings.sharing(loggedIn.user.getUsername, document))
            
          case Some(t) if t == "history" =>
            Ok(views.html.document.settings.history(loggedIn.user.getUsername, document))
            
          case _ =>
            Ok(views.html.document.settings.metadata(loggedIn.user.getUsername, document)) 
        }
      }
    )
  }

}
