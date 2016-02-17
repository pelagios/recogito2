package controllers

import database.DB
import play.api.mvc.{ Request, Controller, AnyContent }
import play.api.Logger

/** Helper trait so we can hand the injected DB down to the Security trait **/
trait HasDatabase { def db: DB }

/** Currently (mostly) a placeholder for future common Controller functionality **/
abstract class AbstractController extends Controller with HasDatabase {

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
