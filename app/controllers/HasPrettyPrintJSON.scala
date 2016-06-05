package controllers

import play.api.mvc.{ AnyContent, Controller, Request }
import play.api.libs.json.{ Json, JsValue }
import scala.util.Try

/** Helper for creating pretty-printed JSON responses with proper content-type header **/
trait HasPrettyPrintJSON { self: Controller =>

  /** Pretty print URL param name **/
  private val PRETTY = "pretty"

  protected def jsonOk(obj: JsValue)(implicit request: Request[AnyContent]) = {
    val pretty = Try(request.queryString.get(PRETTY).map(_.head.toBoolean).getOrElse(false)).getOrElse(false)
    if (pretty)
      Ok(Json.prettyPrint(obj)).withHeaders(("Content-Type", "application/json; charset=utf-8"))
    else
      Ok(obj)
  }

}
