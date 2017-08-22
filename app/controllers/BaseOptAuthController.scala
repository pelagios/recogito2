package controllers

import jp.t2v.lab.play2.auth.OptionalAuthElement
import models.document.{ DocumentAccessLevel, DocumentInfo, DocumentService }
import models.generated.tables.records.{ DocumentFilepartRecord, DocumentRecord, UserRecord }
import models.user.UserService
import play.api.Configuration
import play.api.mvc.{ AnyContent, Request, Result }
import scala.concurrent.{ ExecutionContext, Future }

abstract class BaseOptAuthController(
    config: Configuration,
    documents: DocumentService,
    users: UserService
  ) extends BaseController(config, users) with OptionalAuthElement {
  
  
  /** Helper that covers the boilerplate for all document views
    *
    * Just hand this method a function that produces an HTTP OK result for a document, while
    * the method handles ForbiddenPage/Not Found error cases.
    */
  protected def documentResponse(
      documentId: String,
      maybeUser: Option[UserRecord],
      response: (DocumentInfo, DocumentAccessLevel) => Future[Result]
    )(implicit ctx: ExecutionContext) = {

    documents.getExtendedInfo(documentId, maybeUser.map(_.getUsername)).flatMap(_ match {
      case Some((doc, accesslevel)) => response(doc, accesslevel)
      case None => Future.successful(NotFoundPage)
    }).recover { case t =>
      t.printStackTrace()
      InternalServerError(t.getMessage)    
    }
  }
  
  /** Helper that covers the boilerplate for document views requiring read access **/
  protected def documentReadResponse(
      documentId: String,
      maybeUser: Option[UserRecord],
      response: (DocumentInfo, DocumentAccessLevel) => Future[Result]
    )(implicit ctx: ExecutionContext, request: Request[AnyContent])  = {
    
    documentResponse(documentId, maybeUser, { case (doc, accesslevel) =>
      if (accesslevel.canRead)
        response(doc, accesslevel)
      else if (maybeUser.isEmpty) // No read rights - but user is not logged in yet 
        authenticationFailed(request)        
      else
        Future.successful(ForbiddenPage)
    })
  }
  
  /** Helper that covers the boilerplate for all document part views **/
  protected def documentPartResponse(
      documentId: String,
      partNo: Int,
      maybeUser: Option[UserRecord],
      response: (DocumentInfo, DocumentFilepartRecord, DocumentAccessLevel) => Future[Result]
    )(implicit ctx: ExecutionContext, request: Request[AnyContent]) = {
    
    documentReadResponse(documentId, maybeUser, { case (doc, accesslevel) =>
      doc.fileparts.find(_.getSequenceNo == partNo) match {
        case None => Future.successful(NotFoundPage)
        case Some(part) => response(doc, part, accesslevel)
      }
    })
  }
  
}