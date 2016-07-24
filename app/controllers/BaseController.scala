package controllers

import play.api.cache.CacheApi
import play.api.mvc.{ AnyContent, Controller, Request }
import storage.DB

/** Helper trait so we can hand the injected DB into other traits **/
trait HasDatabase { def db: DB }

/** Helper trait so we can hand the injected Cache into other traits **/
trait HasCache { def cache: CacheApi }

/** Common Controller functionality for convenience **/
abstract class BaseController extends Controller {
  
  protected val NotFoundPage = NotFound(views.html.error404())

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
