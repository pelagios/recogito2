package controllers

import java.io.File
import jp.t2v.lab.play2.auth.AuthElement
import models.document.DocumentService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.mvc.{ Request, Result, Controller, AnyContent }
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ Json, JsValue }
import play.api.libs.Files.TemporaryFile
import storage.DB
import scala.util.Try

/** Helper trait so we can hand the injected DB into other traits **/
trait HasDatabase { def db: DB }

/** Helper trait so we can hand the injected Cache into other traits **/
trait HasCache { def cache: CacheApi }

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

/** Currently (mostly) a placeholder for future common Controller functionality **/
abstract class BaseController extends Controller with HasCache with HasDatabase with AuthElement with Security {

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

  /** Helper that covers the boilerplate for all document views
    *
    * Just hand this method a function that produces an HTTP OK result for a document, while
    * the method handles Forbidden/Not Found error cases.
    */
  protected def renderDocumentResponse(docId: String, user: String,
      response: (DocumentRecord, Seq[DocumentFilepartRecord]) => Result)(implicit cache: CacheApi, db: DB) = {

    DocumentService.findByIdWithFileparts(docId).map(_ match {
      case Some((document, fileparts)) => {
        // Verify if the user is allowed to access this document - TODO what about shared content?
        if (document.getOwner == user)

          response(document, fileparts)

        else
          Forbidden
      }

      case None =>
        // No document with that ID found in DB
        NotFound
    }).recover { case t =>
      t.printStackTrace()
      InternalServerError(t.getMessage)
    }
  }

  /** Helper that covers the boilerplate for all document part views **/
  protected def renderDocumentPartResponse(docId: String, partNo: Int, user: String,
      response: (DocumentRecord, Seq[DocumentFilepartRecord], DocumentFilepartRecord) => Result)(implicit cache: CacheApi, db: DB) = {

    renderDocumentResponse(docId, user, { case (document, fileparts) =>
      val selectedPart = fileparts.filter(_.getSequenceNo == partNo)
      if (selectedPart.isEmpty) {
        NotFound
      } else if (selectedPart.size == 1) {

        response(document, fileparts, selectedPart.head)

      } else {
        // More than one part with this sequence number - DB integrity broken!
        throw new Exception("Invalid ocument part")
      }
    })
  }

}
