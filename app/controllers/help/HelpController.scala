package controllers.help

import controllers.{ HasVisitLogging, WebJarAssets }
import javax.inject.Inject
import models.visit.VisitService
import play.api.mvc.{ Action, Controller, RequestHeader }
import play.twirl.api.HtmlFormat

class HelpController @Inject() (
    implicit val visits: VisitService,
    implicit val webjars: WebJarAssets
  ) extends Controller with HasVisitLogging {

  private def result(template: HtmlFormat.Appendable)(implicit request: RequestHeader) = {
    logPageView()
    Ok(template)
  }
  
  def index         = Action { Redirect(routes.HelpController.showTutorial()) }

  def showAbout     = Action { implicit request => result(views.html.help.about()) }

  def showFAQ       = Action { implicit request => result(views.html.help.faq())   }

  def showTutorial  = Action { implicit request => result(views.html.help.tutorial()) }

}
