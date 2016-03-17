package controllers

import jp.t2v.lab.play2.auth.AuthElement
import models.content.DocumentService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import play.api.cache.CacheApi
import play.api.mvc.{ Request, Result, Controller, AnyContent }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import storage.DB

/** Helper trait so we can hand the injected Cache and DB down to the Security trait **/
trait HasCacheAndDatabase {
 
  def cache: CacheApi
  
  def db: DB
  
} 

/** Currently (mostly) a placeholder for future common Controller functionality **/
abstract class AbstractController extends Controller with HasCacheAndDatabase with AuthElement with Security {

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
      response: (DocumentRecord, Seq[DocumentFilepartRecord], DocumentFilepartRecord) => Result)(implicit cache: CacheApi, db:DB) = {
    
    DocumentService.findByIdWithFileparts(docId).map(_ match {
      case Some((document, fileparts)) => {
        if (document.getOwner == user) {
          val selectedPart = fileparts.filter(_.getSequenceNo == partNo)
          if (selectedPart.isEmpty) {
            NotFound
          } else if (selectedPart.size == 1) {
            
            response(document, fileparts, selectedPart.head)
            
          } else {
            // More than one part with this sequence number - DB integrity broken!
            throw new Exception("Invalid ocument part")
          }
        } else {
          Forbidden
        }
      }
      
      case None =>
        NotFound
    }).recover { case t =>
      t.printStackTrace()
      InternalServerError(t.getMessage)
    }
  }

}
