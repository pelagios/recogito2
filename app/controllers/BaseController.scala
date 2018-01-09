package controllers

import models.user.UserService
import play.api.Configuration
import play.api.mvc.{ AnyContent, AbstractController, ControllerComponents, Request }

trait HasConfig { def config: Configuration }

trait HasUserService { def users: UserService }

/** Common Controller functionality for convenience **/
abstract class BaseController(
    components: ControllerComponents, 
    config: Configuration, 
    users: UserService
  ) extends AbstractController(components) with HasConfig with HasUserService {

  protected val NotFoundPage = NotFound(views.html.error404())
  
  protected val ForbiddenPage = Forbidden(views.html.error403())
   
  /** Returns the value of the specified query string parameter **/
  protected def getQueryParam(key: String)(implicit request: Request[AnyContent]): Option[String] =
    request.queryString.get(key).map(_.head)

  /** Returns the value of the specified form parameter **/
  protected def getFormParam(key: String)(implicit request: Request[AnyContent]): Option[String] =
    request.body.asFormUrlEncoded match {
      case Some(form) =>
        form.get(key).map(_.head)

      case None =>
        None
    }

  /** Returns the value of the specified query string or form parameter **/
  protected def getParam(key: String)(implicit request: Request[AnyContent]): Option[String] =
    Seq(getFormParam(key), getQueryParam(key)).flatten.headOption

  /** Convenience function that checks if the specified (query string or form) param has the specified value **/
  protected def checkParamValue(key: String, value: String)(implicit request: Request[AnyContent]): Boolean =
    getParam(key).equals(Some(value))
  
}
