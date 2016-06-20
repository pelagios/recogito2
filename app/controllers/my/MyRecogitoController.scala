package controllers.my

import controllers.{ HasCache, HasDatabase, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.OptionalAuthElement
import models.user.UserService
import models.document.DocumentService
import play.api.Play
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import storage.DB
import scala.concurrent.Future

class MyRecogitoController @Inject() (implicit val cache: CacheApi, val db: DB) 
  extends Controller with HasCache with HasDatabase with OptionalAuthElement with Security {
  
  // TODO this may depend on user in the future
  private lazy val QUOTA = Play.current.configuration.getInt("recogito.upload.quota").getOrElse(200)

  /** A convenience '/my' route that redirects to the personal index **/
  def my = StackAction { implicit request =>
    loggedIn match {
      case Some(userWithRoles) =>
        Redirect(routes.MyRecogitoController.index(userWithRoles.user.getUsername))
        
      case None =>
        // Not logged in - go to log in and then come back here
        Redirect(controllers.landing.routes.LoginLogoutController.showLoginForm)
          .withSession("access_uri" -> routes.MyRecogitoController.my.url)
    }
  }
  
  def index(usernameInPath: String) = AsyncStack { implicit request =>    
    // If the user is logged in & the name in the path == username it's the profile owner
    val isProfileOwner = loggedIn match {
      case Some(userWithRoles) => userWithRoles.user.getUsername.equalsIgnoreCase(usernameInPath)
      case None => false
    }
    
    DocumentService.findByOwner(usernameInPath, !isProfileOwner).flatMap(documents =>
      if (isProfileOwner) {
        // Personal space
        val user = loggedIn.get.user
        DocumentService.countSharedDocuments(user.getUsername).map { sharedCount =>
          Ok(views.html.my.index_private(user, UserService.getUsedDiskspaceKB(user.getUsername), QUOTA, documents, sharedCount)) }
      } else {
        // Public profile
        UserService.findByUsername(usernameInPath).map(_ match {
          case Some(userWithRoles) =>
            // Show public profile
            Ok(views.html.my.index_public(userWithRoles.user, documents))
              
          case None =>
            // There is no user with the specified name
            NotFound
        })
      }
    ) 
  }

}
