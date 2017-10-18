package controllers.landing

import controllers.{ HasConfig, HasUserService, HasVisitLogging, HasPrettyPrintJSON, Security, WebJarAssets }
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.OptionalAuthElement
import models.annotation.AnnotationService
import models.contribution.ContributionService
import models.user.UserService
import models.visit.VisitService
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }
import scala.concurrent.ExecutionContext

@Singleton
class LandingController @Inject() (
    val config: Configuration,
    val annotations: AnnotationService,
    val contributions: ContributionService,
    val users: UserService,
    implicit val ctx: ExecutionContext,
    implicit val visits: VisitService,
    implicit val webjars: WebJarAssets
) extends Controller with HasConfig with HasUserService with OptionalAuthElement with Security with HasVisitLogging with HasPrettyPrintJSON {

  def index = StackAction { implicit request =>
    loggedIn match {
      case Some(user) =>
        Redirect(controllers.my.routes.MyRecogitoController.index(user.username, None, None, None, None))

      case None =>
        logPageView()
        Ok(views.html.landing.index())
    }
  }
  
  def getStats() = Action.async { implicit request =>
    val fAnnotations = annotations.countTotal()
    val fEdits = contributions.countLast24hrs()
    val fUsers = users.countUsers()
    
    val f = for {
      annotations <- fAnnotations
      edits <- fEdits
      users <- fUsers
    } yield (annotations, edits, users)
    
    f.map { case (annotations, edits, users) =>
      jsonOk(Json.obj(
        "annotations" -> annotations,
        "edits" -> edits,
        "users" -> users
      ))
    }
  }

}
