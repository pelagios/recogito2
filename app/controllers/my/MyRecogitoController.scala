package controllers.my

import controllers.{ BaseController, ControllerContext, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.OptionalAuthElement
import models.Page
import models.user.UserService
import models.document.DocumentService
import models.generated.tables.records.{ DocumentRecord, UserRecord }
import play.api.mvc.RequestHeader
import scala.concurrent.Future

class MyRecogitoController @Inject() (implicit val ctx: ControllerContext) 
  extends BaseController with OptionalAuthElement with Security {

  // TODO this may depend on user in the future
  private lazy val QUOTA = ctx.config.getInt("recogito.upload.quota").getOrElse(200)

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
      case None => NotFoundPage
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
