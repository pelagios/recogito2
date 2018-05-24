package controllers.help

import controllers.HasVisitLogging
import javax.inject.{Inject, Singleton}
import services.visit.VisitService
import org.webjars.play.WebJarsUtil
import play.api.{Configuration, Environment}
import play.api.mvc.{Action, AbstractController, ControllerComponents, RequestHeader}
import play.twirl.api.HtmlFormat
import scala.io.Source
import scala.util.Try

@Singleton
class HelpController @Inject() (
    val components: ControllerComponents,
    val config: Configuration,
    val env: Environment,
    implicit val visits: VisitService,
    implicit val webjars: WebJarsUtil
  ) extends AbstractController(components) with HasVisitLogging {

  private def result(template: HtmlFormat.Appendable)(implicit request: RequestHeader) = {
    logPageView()
    Ok(template)
  }

  def index = Action { Redirect(routes.HelpController.showTutorial()) }

  def showTutorial  = Action { implicit request => result(views.html.help.tutorial()) }

  def showLocalizedTutorial(lang: String) = Action { implicit request =>
    lang.toUpperCase match {
      case "DE" => result(views.html.help.tutorial_de())
      case "ES" => result(views.html.help.tutorial_es())
      case "FA" => result(views.html.help.tutorial_fa())
      case "FR" => result(views.html.help.tutorial_fr())
      case "IT" => result(views.html.help.tutorial_it())
      case "NL" => result(views.html.help.tutorial_nl())
      case _ => NotFound(views.html.error404())
    }
  }

  def showFAQ = Action { implicit request => result(views.html.help.faq()) }

  def showAbout = Action { implicit request =>
    val imprint = Try(Source.fromFile(env.getFile("conf/imprint"))).toOption
      .map { _.getLines.mkString("\n") }

    val adminEmail = config.get[String]("admin.email")

    result(views.html.help.about(imprint, adminEmail))
  }

}
