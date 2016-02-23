package controllers.landing

import play.api.mvc.{ Action, Controller }

/** Doesn't do a lot yet, except serve the main index page **/
class LandingController extends Controller {

  def index = Action {
    Ok(views.html.landing.index())
  }

}
