package controllers.my

import controllers.{ BaseController, Security, WebJarAssets }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.OptionalAuthElement
import models.{ Page, SortOrder }
import models.annotation.AnnotationService
import models.contribution.ContributionService
import models.user.UserService
import models.document.DocumentService
import models.generated.tables.records.{ DocumentRecord, UserRecord }
import play.api.Configuration
import play.api.mvc.RequestHeader
import scala.concurrent.{ ExecutionContext, Future }

class MyRecogitoController @Inject() (
    val annotations: AnnotationService,
    val contributions: ContributionService,
    val documents: DocumentService,
    val users: UserService,
    val config: Configuration,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarAssets
  ) extends BaseController(config, users) with OptionalAuthElement {

  private lazy val DOCUMENTS_PER_PAGE = 10

  /** A convenience '/my' route that redirects to the personal index **/
  def my = StackAction { implicit request =>
    loggedIn match {
      case Some(userWithRoles) =>
        Redirect(routes.MyRecogitoController.index(userWithRoles.user.getUsername.toLowerCase, None, None, None, None))

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

  private def renderMyDocuments(user: UserRecord, usedSpace: Long, offset: Int, sortBy: Option[String], sortOrder: Option[SortOrder])(implicit request: RequestHeader) = {
    // Fetch properties located in the DB
    val fMyDocuments = documents.findByOwner(user.getUsername, false, offset, DOCUMENTS_PER_PAGE, sortBy, sortOrder)
    val fSharedCount = documents.countBySharedWith(user.getUsername)
    val fDBProps = for {
      myDocuments <- fMyDocuments
      sharedCount <- fSharedCount
    } yield (myDocuments, sharedCount)
    
    // Fetch properties located in the index
    val fAllProps = fDBProps.flatMap { case (myDocuments, sharedCount) =>
      val docIds = myDocuments.items.map(_.getId)
      val fLastEdits = Future.sequence(docIds.map(id => contributions.getLastContribution(id).map((id, _))))
      val fAnnotationsPerDoc = Future.sequence(docIds.map(id => annotations.countByDocId(id).map((id, _))))
      
      for {
        lastEdits <- fLastEdits
        annotationsPerDoc <- fAnnotationsPerDoc
      } yield (myDocuments, sharedCount, lastEdits.toMap, annotationsPerDoc.toMap)
    }
    
    fAllProps.map { case (myDocuments, sharedCount, lastEdits, annotationsPerDoc) =>
      val tableRow = myDocuments.map { doc =>
        val lastEdit = lastEdits.find(_._1 == doc.getId).flatMap(_._2)
        val annotations = annotationsPerDoc.find(_._1 == doc.getId).map(_._2).getOrElse(0l)
        (doc, lastEdit, annotations)
      }
      
      Ok(views.html.my.my_private(user, usedSpace, tableRow, sharedCount, sortBy, sortOrder))
    }
  }

  private def renderSharedWithMe(user: UserRecord, usedSpace: Long, offset: Int, sortBy: Option[String], sortOrder: Option[SortOrder])(implicit request: RequestHeader) = {
    val f = for {
      myDocsCount <- documents.countByOwner(user.getUsername, false)
      docsSharedWithMe <- documents.findBySharedWith(user.getUsername, offset, DOCUMENTS_PER_PAGE, sortBy, sortOrder)
    } yield (myDocsCount, docsSharedWithMe)

    f.map { case (myDocsCount, docsSharedWithMe) =>
      Ok(views.html.my.my_shared(user, usedSpace, myDocsCount, docsSharedWithMe, sortBy, sortOrder))
    }
  }

  def index(usernameInPath: String, tab: Option[String], page: Option[Int], sortBy: Option[String], order: Option[String]) = AsyncStack { implicit request =>
    // If the user is logged in & the name in the path == username it's the profile owner
    val isProfileOwner = loggedIn match {
      case Some(userWithRoles) => userWithRoles.user.getUsername.equalsIgnoreCase(usernameInPath)
      case None => false
    }
    
    val offset = (page.getOrElse(1) - 1) * DOCUMENTS_PER_PAGE
    
    val sortOrder = order.flatMap(SortOrder.fromString(_))

    if (isProfileOwner) {
      val user = loggedIn.get.user
      val usedSpace = users.getUsedDiskspaceKB(user.getUsername)

      tab match {
        case Some(t) if t.equals("shared") => renderSharedWithMe(user, usedSpace, offset, sortBy, sortOrder)
        case _ => renderMyDocuments(user, usedSpace, offset, sortBy, sortOrder)
      }
    } else {
      renderPublicProfile(usernameInPath, loggedIn.map(_.user))
    }
  }

}
