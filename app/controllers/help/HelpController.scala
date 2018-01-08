package controllers.help

import controllers.HasVisitLogging
import javax.inject.{ Inject, Singleton }
import models.visit.VisitService
import org.webjars.play.WebJarsUtil
import play.api.mvc.{ Action, Controller, RequestHeader }
import play.twirl.api.HtmlFormat

@Singleton
class HelpController @Inject() (
    implicit val visits: VisitService,
    implicit val webjars: WebJarsUtil
  ) extends Controller with HasVisitLogging {

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
      case "IT" => result(views.html.help.tutorial_it())
      case _ => NotFound(views.html.error404())
    }
  }

  def showFAQ = Action { implicit request => result(views.html.help.faq()) }

  def showAbout = Action { implicit request => result(views.html.help.about()) }

}
