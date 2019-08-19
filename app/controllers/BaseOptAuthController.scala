package controllers

import services.RuntimeAccessLevel
import services.document.{ExtendedDocumentMetadata, DocumentService}
import services.generated.tables.records.{DocumentFilepartRecord, DocumentRecord}
import services.user.{User, UserService}
import play.api.Configuration
import play.api.mvc.{AnyContent, ControllerComponents, Request, Result}
import scala.concurrent.{ExecutionContext, Future}

abstract class BaseOptAuthController(
    components: ControllerComponents,
    config: Configuration,
    documents: DocumentService,
    users: UserService
  ) extends BaseController(components, config, users) {
 
  /** Helper that covers the boilerplate for all document views
    *
    * Just hand this method a function that produces an HTTP OK result for a document, while
    * the method handles ForbiddenPage/Not Found error cases.
    */
  protected def documentResponse(
      documentId: String,
      maybeUser: Option[User],
      response: (ExtendedDocumentMetadata, RuntimeAccessLevel) => Future[Result]
    )(implicit ctx: ExecutionContext) = {

    documents.getExtendedMeta(documentId, maybeUser.map(_.username)).flatMap(_ match {
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
    maybeUser: Option[User],
    response: (ExtendedDocumentMetadata, RuntimeAccessLevel) => Future[Result]
  )(implicit ctx: ExecutionContext, request: Request[AnyContent])  = {
    
    documentResponse(documentId, maybeUser, { case (doc, accesslevel) =>
      if (accesslevel.canReadData)
        response(doc, accesslevel)
      else if (maybeUser.isEmpty) // No read rights - but user is not logged in yet 
        Future.successful(
          Redirect(controllers.landing.routes.LoginLogoutController.showLoginForm(None))
            .withSession("access_uri" -> request.uri)
        )  
      else
        Future.successful(ForbiddenPage)
    })
  }
  
  /** Helper that covers the boilerplate for all document part views **/
  protected def documentPartResponse(
      documentId: String,
      partNo: Int,
      maybeUser: Option[User],
      response: (ExtendedDocumentMetadata, DocumentFilepartRecord, RuntimeAccessLevel) => Future[Result]
    )(implicit ctx: ExecutionContext, request: Request[AnyContent]) = {
    
    documentReadResponse(documentId, maybeUser, { case (doc, accesslevel) =>
      doc.fileparts.find(_.getSequenceNo == partNo) match {
        case None => Future.successful(NotFoundPage)
        case Some(part) => response(doc, part, accesslevel)
      }
    })
  }
  
} 