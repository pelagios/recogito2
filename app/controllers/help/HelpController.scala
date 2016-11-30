package controllers.help

import play.api.mvc.Controller
import play.api.mvc.Action

class HelpController extends Controller {

  // TODO track visits

  def index         = Action { Redirect(routes.HelpController.showTutorial()) }

  def showAbout     = Action { implicit request => Ok(views.html.help.about()) }

  def showFAQ       = Action { implicit request => Ok(views.html.help.faq())   }

  def showTutorial  = Action { implicit request => Ok(views.html.help.tutorial()) }

}
