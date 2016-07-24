package controllers

import jp.t2v.lab.play2.auth.AuthElement
import models.document.{ DocumentAccessLevel, DocumentService }
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.mvc.Result
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import storage.DB

abstract class BaseAuthController extends BaseController with HasCache with HasDatabase with AuthElement with Security {
  
  /** Helper that covers the boilerplate for all document views
    *
    * Just hand this method a function that produces an HTTP OK result for a document, while
    * the method handles ForbiddenPage/Not Found error cases.
    */
  protected def documentResponse(docId: String, username: String,
      response: (DocumentRecord, Seq[DocumentFilepartRecord], DocumentAccessLevel) => Result)(implicit cache: CacheApi, db: DB) = {

    DocumentService.findByIdWithFileparts(docId, Some(username)).map(_ match {
      case Some((document, fileparts, accesslevel)) => {
        if (accesslevel.canRead)
          // As long as there are read rights we'll allow access here - the response
          // method must handle more fine-grained access by itself
          response(document, fileparts, accesslevel)
        else
          ForbiddenPage
      }

      case None =>
        // No document with that ID found in DB
        NotFoundPage
    }).recover { case t =>
      t.printStackTrace()
      InternalServerError(t.getMessage)
    }
  }

  /** Helper that covers the boilerplate for all document part views **/
  protected def documentPartResponse(docId: String, partNo: Int, username: String,
      response: (DocumentRecord, Seq[DocumentFilepartRecord], DocumentFilepartRecord, DocumentAccessLevel) => Result)(implicit cache: CacheApi, db: DB) = {

    documentResponse(docId, username, { case (document, fileparts, accesslevel) =>
      val selectedPart = fileparts.filter(_.getSequenceNo == partNo)
      if (selectedPart.isEmpty)
        NotFoundPage
      else if (selectedPart.size == 1)
        response(document, fileparts, selectedPart.head, accesslevel)
      else
        // More than one part with this sequence number - DB integrity broken!
        throw new Exception("Invalid document part")
    })
  }
  
}