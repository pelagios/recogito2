package controllers

import javax.inject.{ Inject, Provider, Singleton }
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.mvc.{ AnyContent, Controller, Request }
import scala.concurrent.ExecutionContext
import storage.DB

/** Helper class to reduce Controller boilerplate **/ 
class ControllerContext(val cache: CacheApi, val config: Configuration, val db: DB, val ec: ExecutionContext, val webjars: WebJarAssets) 

@Singleton
class ControllerContextProvider @Inject() (val cache: CacheApi, val config: Configuration, val db: DB, val ec: ExecutionContext, val webjars: WebJarAssets) 
  extends Provider[ControllerContext] {
  
  def get = new ControllerContext(cache, config, db, ec, webjars)
  
}

trait HasContext { def ctx: ControllerContext }

/** Common Controller functionality for convenience **/
abstract class BaseController extends Controller with HasContext {
  
  implicit def controllerContextToCache(implicit ctx: ControllerContext) = ctx.cache
  implicit def controllerContextToConfig(implicit ctx: ControllerContext) = ctx.config
  implicit def controllerContextToDB(implicit ctx: ControllerContext) = ctx.db
  implicit def controllerContextToExecutionContext(implicit ctx: ControllerContext) = ctx.ec
  implicit def controllerContextToWebJars(implicit ctx: ControllerContext) = ctx.webjars
  
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
