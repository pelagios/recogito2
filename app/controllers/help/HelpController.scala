package controllers.help

import play.api.mvc.Controller
import play.api.mvc.Action

class HelpController extends Controller {
  
  def showAbout = Action { implicit request => Ok(views.html.help.about()) }
  
  def showFAQ   = Action { implicit request => Ok(views.html.help.faq())   }
    
  def showGuide = Action { implicit request => Ok(views.html.help.guide()) }
  
}