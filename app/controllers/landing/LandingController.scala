package controllers.landing

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{ HasConfig, HasUserService, HasVisitLogging, HasPrettyPrintJSON, Security }
import javax.inject.{ Inject, Singleton }
import services.annotation.AnnotationService
import services.contribution.ContributionService
import services.user.UserService
import services.visit.VisitService
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AbstractController, ControllerComponents}
import scala.concurrent.ExecutionContext

@Singleton
class LandingController @Inject() (
    val components: ControllerComponents,
    val config: Configuration,
    val annotations: AnnotationService,
    val contributions: ContributionService,
    val users: UserService,
    val silhouette: Silhouette[Security.Env],
    implicit val ctx: ExecutionContext,
    implicit val visits: VisitService,
    implicit val webjars: WebJarsUtil
) extends AbstractController(components) with HasConfig with HasUserService with HasVisitLogging with HasPrettyPrintJSON with I18nSupport {
  
  def index = silhouette.UserAwareAction { implicit request =>    
    request.identity match {
      case Some(user) =>
        Redirect(controllers.my.routes.MyRecogitoController.index(user.username, None, None, None, None, None))

      case None =>
        logPageView()
        Ok(views.html.landing.index())
    }
  }
  
  def getStats() = silhouette.UnsecuredAction.async { implicit request =>
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
