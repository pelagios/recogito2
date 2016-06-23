package controllers.my

import controllers.WebJarAssets
import controllers.{ HasCache, HasDatabase, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.OptionalAuthElement
import models.Page
import models.user.UserService
import models.document.DocumentService
import models.generated.tables.records.{ DocumentRecord, UserRecord }
import models.generated.tables.records.UserRecord
import play.api.Play
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{ Controller, RequestHeader }
import storage.DB
import scala.concurrent.Future

class MyRecogitoController @Inject() (implicit val cache: CacheApi, val db: DB, val webjars: WebJarAssets) 
  extends Controller with HasCache with HasDatabase with OptionalAuthElement with Security {

  // TODO this may depend on user in the future
  private lazy val QUOTA = Play.current.configuration.getInt("recogito.upload.quota").getOrElse(200)

  /** A convenience '/my' route that redirects to the personal index **/
  def my = StackAction { implicit request =>
    loggedIn match {
      case Some(userWithRoles) =>
        Redirect(routes.MyRecogitoController.index(userWithRoles.user.getUsername, None))
        
      case None =>
        // Not logged in - go to log in and then come back here
        Redirect(controllers.landing.routes.LoginLogoutController.showLoginForm)
          .withSession("access_uri" -> routes.MyRecogitoController.my.url)
    }
  }
  
  private def renderPublicProfile(username: String) = {
    val f = for {
      userWithRoles   <- UserService.findByUsername(username)
      publicDocuments <- if (userWithRoles.isDefined)
                           DocumentService.findByOwner(userWithRoles.get.user.getUsername, true)
                         else
                           Future.successful(Page.empty[DocumentRecord])
    } yield (userWithRoles, publicDocuments)
    
    f.map { case (userWithRoles, publicDocuments) => userWithRoles match {
      case Some(u) => Ok(views.html.my.my_public(u.user, publicDocuments))
      case None => NotFound
    }}  
  }
  
  private def renderMyDocuments(user: UserRecord, usedSpace: Long)(implicit request: RequestHeader) = {
    val f = for {
      myDocuments <- DocumentService.findByOwner(user.getUsername, false)
      sharedCount <- DocumentService.countBySharedWith(user.getUsername)
    } yield (myDocuments, sharedCount)
    
    f.map { case (myDocuments, sharedCount) =>
      Ok(views.html.my.my_private(user, usedSpace, QUOTA, myDocuments, sharedCount))
    }
  }
  
  private def renderSharedWithMe(user: UserRecord, usedSpace: Long)(implicit request: RequestHeader) = {
    val f = for {
      myDocsCount <- DocumentService.countByOwner(user.getUsername, false)
      docsSharedWithMe <- DocumentService.findBySharedWith(user.getUsername)
    } yield (myDocsCount, docsSharedWithMe)
    
    f.map { case (myDocsCount, docsSharedWithMe) =>
      Ok(views.html.my.my_shared(user, usedSpace, QUOTA, myDocsCount, docsSharedWithMe))
    }  
  }
  
  def index(usernameInPath: String, tab: Option[String]) = AsyncStack { implicit request =>    
    // If the user is logged in & the name in the path == username it's the profile owner
    val isProfileOwner = loggedIn match {
      case Some(userWithRoles) => userWithRoles.user.getUsername.equalsIgnoreCase(usernameInPath)
      case None => false
    }
    
    if (isProfileOwner) {
      val user = loggedIn.get.user
      val usedSpace = UserService.getUsedDiskspaceKB(user.getUsername)
      
      tab match {
        case Some(t) if t.equals("shared") => renderSharedWithMe(user, usedSpace)
        case _ => renderMyDocuments(user, usedSpace)
      }
    } else {
      renderPublicProfile(usernameInPath)
    }
  }

}
