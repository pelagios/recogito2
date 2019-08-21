package controllers.landing

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{ HasConfig, HasUserService, HasVisitLogging, HasPrettyPrintJSON, Security }
import java.io.FileInputStream
import javax.inject.{ Inject, Singleton }
import java.net.URI
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, JsObject}
import play.api.mvc.{Action, AbstractController, ControllerComponents}
import scala.concurrent.ExecutionContext
import services.annotation.AnnotationService
import services.contribution.ContributionService
import services.document.DocumentService
import services.user.UserService
import services.visit.VisitService

@Singleton
class LandingController @Inject() (
    val components: ControllerComponents,
    val config: Configuration,
    val annotations: AnnotationService,
    val contributions: ContributionService,
    val documents: DocumentService,
    val users: UserService,
    val silhouette: Silhouette[Security.Env],
    implicit val ctx: ExecutionContext,
    implicit val visits: VisitService,
    implicit val webjars: WebJarsUtil
) extends AbstractController(components) with HasConfig with HasUserService with HasVisitLogging with HasPrettyPrintJSON with I18nSupport {

  def index = silhouette.UserAwareAction { implicit request =>
    // Temporary hack only
    request.queryString.get("lang").flatMap(_.headOption) match {
      case Some(lang) =>
        Redirect(routes.LandingController.index).withLang(play.api.i18n.Lang(lang))
      case None =>
        request.identity match {
          case Some(user) =>
            Redirect(controllers.my.routes.WorkspaceController.workspace(user.username))

          case None =>
            logPageView()
            Ok(views.html.landing.index())
        }
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

  def sitemap() = Action.async { implicit request =>
    documents.listOwnersWithPublicDocuments().map { users =>
      val baseURL = routes.LandingController.index().absoluteURL()
      val sitemap = users.map(user => s"${baseURL}${user}").mkString("\n")
      Ok(sitemap).as("text/plain")
    }
  }

  def robots() = Action { implicit request =>
    val sitemapURL = routes.LandingController.sitemap().absoluteURL()
    Ok(s"SITEMAP: ${sitemapURL}").as("text/plain")
  }

  def swaggerConfig() = Action { implicit request => 
    val json = Json.parse(new FileInputStream("conf/swagger.json"))
    val baseURL = new URI(routes.LandingController.index.absoluteURL)
    val host = if (baseURL.getPort == -1) baseURL.getHost else s"${baseURL.getHost}:${baseURL.getPort}"
    jsonOk(json.as[JsObject] ++ Json.obj("host" -> host))
  }

}
