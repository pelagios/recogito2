package controllers.help

import play.api.mvc.Controller
import play.api.mvc.Action

class HelpController extends Controller {
  
  def index     = Action { Redirect(routes.HelpController.showTour()) }
  
  def showAbout = Action { implicit request => Ok(views.html.help.about()) }
  
  def showFAQ   = Action { implicit request => Ok(views.html.help.faq())   }
    
  def showTour  = Action { implicit request => Ok(views.html.help.tour()) }
  
}