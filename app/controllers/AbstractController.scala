package controllers

import models.content.DocumentService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import play.api.mvc.{ Request, Result, Controller, AnyContent }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import storage.DB

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
    
  /** Helper that covers the boilerplate for all document views
    *
    * Just hand this method a function that produces an HTTP OK result for a document, while
    * the method handles Forbidden/Not Found error cases.
    */
  protected def renderDocumentResponse(docId: Int, user: String, response: (DocumentRecord, Seq[DocumentFilepartRecord]) => Result)(implicit db: DB) = {
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
    })
  }

}
