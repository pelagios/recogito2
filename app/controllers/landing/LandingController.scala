package controllers.landing

import play.api.mvc.{ Action, Controller }

class LandingController extends Controller {

  def index = Action {
    Ok(views.html.landing.index())
  }

}
