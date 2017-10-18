package controllers

import jp.t2v.lab.play2.auth.AuthElement
import models.document.{ DocumentAccessLevel, DocumentInfo, DocumentService }
import models.generated.tables.records.{ DocumentFilepartRecord, DocumentRecord }
import models.user.{ User, UserService }
import play.api.Configuration
import play.api.mvc.Result
import scala.concurrent.ExecutionContext

abstract class BaseAuthController(
    config: Configuration,
    documents: DocumentService,
    users: UserService
  ) extends BaseController(config, users) with AuthElement {
  
  /** Helper that covers the boilerplate for all document views
    *
    * Just hand this method a function that produces an HTTP OK result for a document, while
    * the method handles ForbiddenPage/Not Found error cases.
    */
  protected def documentResponse(
      docId: String,
      user: User,
      response: (DocumentInfo, DocumentAccessLevel) => Result
    )(implicit ctx: ExecutionContext) = {

    documents.getExtendedInfo(docId, Some(user.username)).map(_ match {
      case Some((doc, accesslevel)) => {
        if (accesslevel.canRead)
          // As long as there are read rights we'll allow access here - the response
          // method must handle more fine-grained access by itself
          response(doc, accesslevel)
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
  protected def documentPartResponse(
      docId: String,
      partNo: Int,
      user: User,
      response: (DocumentInfo, DocumentFilepartRecord, DocumentAccessLevel) => Result
    )(implicit ctx: ExecutionContext) = {

    documentResponse(docId, user, { case (doc, accesslevel) =>
      doc.fileparts.find(_.getSequenceNo == partNo) match {
        case None => NotFoundPage
        case Some(part) => response(doc, part, accesslevel)
      }
    })
  }
  
}