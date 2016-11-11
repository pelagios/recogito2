package controllers.my

import controllers.{ BaseController, Security, WebJarAssets }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.OptionalAuthElement
import models.Page
import models.user.UserService
import models.document.DocumentService
import models.generated.tables.records.{ DocumentRecord, UserRecord }
import play.api.Configuration
import play.api.mvc.RequestHeader
import scala.concurrent.{ ExecutionContext, Future }

class MyRecogitoController @Inject() (
    val documents: DocumentService,
    val users: UserService,
    val config: Configuration,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarAssets
  ) extends BaseController(config, users) with OptionalAuthElement {

  // TODO this may depend on user in the future
  private lazy val QUOTA = config.getInt("recogito.upload.quota").getOrElse(200)
  
  private lazy val DOCUMENTS_PER_PAGE = 10

  /** A convenience '/my' route that redirects to the personal index **/
  def my = StackAction { implicit request =>
    loggedIn match {
      case Some(userWithRoles) =>
        Redirect(routes.MyRecogitoController.index(userWithRoles.user.getUsername.toLowerCase, None, None, None))

      case None =>
        // Not logged in - go to log in and then come back here
        Redirect(controllers.landing.routes.LoginLogoutController.showLoginForm(None))
          .withSession("access_uri" -> routes.MyRecogitoController.my.url)
    }
  }

  private def renderPublicProfile(username: String, loggedInUser: Option[UserRecord])(implicit request: RequestHeader) = {
    val f = for {
      userWithRoles   <- users.findByUsernameIgnoreCase(username)
      publicDocuments <- if (userWithRoles.isDefined)
                           documents.findByOwner(userWithRoles.get.user.getUsername, true)
                         else
                           Future.successful(Page.empty[DocumentRecord])
    } yield (userWithRoles, publicDocuments)

    f.map { case (userWithRoles, publicDocuments) => userWithRoles match {
      case Some(u) => Ok(views.html.my.my_public(u.user, publicDocuments, loggedInUser))
      case None => NotFoundPage
    }}
  }

  private def renderMyDocuments(user: UserRecord, usedSpace: Long, offset: Int, sortBy: Option[String])(implicit request: RequestHeader) = {
    val f = for {
      myDocuments <- documents.findByOwner(user.getUsername, false, offset, DOCUMENTS_PER_PAGE)
      sharedCount <- documents.countBySharedWith(user.getUsername)
    } yield (myDocuments, sharedCount)

    f.map { case (myDocuments, sharedCount) =>
      Ok(views.html.my.my_private(user, usedSpace, QUOTA, myDocuments, sharedCount))
    }
  }

  private def renderSharedWithMe(user: UserRecord, usedSpace: Long, offset: Int, sortBy: Option[String])(implicit request: RequestHeader) = {
    val f = for {
      myDocsCount <- documents.countByOwner(user.getUsername, false)
      docsSharedWithMe <- documents.findBySharedWith(user.getUsername, offset, DOCUMENTS_PER_PAGE)
    } yield (myDocsCount, docsSharedWithMe)

    f.map { case (myDocsCount, docsSharedWithMe) =>
      Ok(views.html.my.my_shared(user, usedSpace, QUOTA, myDocsCount, docsSharedWithMe))
    }
  }

  def index(usernameInPath: String, tab: Option[String], page: Option[Int], sortBy: Option[String]) = AsyncStack { implicit request =>
    // If the user is logged in & the name in the path == username it's the profile owner
    val isProfileOwner = loggedIn match {
      case Some(userWithRoles) => userWithRoles.user.getUsername.equalsIgnoreCase(usernameInPath)
      case None => false
    }
    
    val offset = (page.getOrElse(1) - 1) * DOCUMENTS_PER_PAGE

    if (isProfileOwner) {
      val user = loggedIn.get.user
      val usedSpace = users.getUsedDiskspaceKB(user.getUsername)

      tab match {
        case Some(t) if t.equals("shared") => renderSharedWithMe(user, usedSpace, offset, sortBy)
        case _ => renderMyDocuments(user, usedSpace, offset, sortBy)
      }
    } else {
      renderPublicProfile(usernameInPath, loggedIn.map(_.user))
    }
  }

}
