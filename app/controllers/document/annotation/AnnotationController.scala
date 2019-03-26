package controllers.document.annotation

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseOptAuthController, HasVisitLogging, HasTEISnippets, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import services.{ContentType, RuntimeAccessLevel}
import services.annotation.{Annotation, AnnotationService}
import services.document.{ExtendedDocumentMetadata, DocumentService}
import services.generated.tables.records.{DocumentFilepartRecord, DocumentRecord, DocumentPreferencesRecord, UserRecord}
import services.user.{User, UserService}
import services.visit.VisitService
import org.webjars.play.WebJarsUtil
import play.api.{Configuration, Logger}
import play.api.i18n.I18nSupport
import play.api.mvc.{ControllerComponents, RequestHeader, Result}
import scala.concurrent.{ExecutionContext, Future}
import storage.uploads.Uploads

@Singleton
class AnnotationController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val annotations: AnnotationService,
  val documents: DocumentService,
  val silhouette: Silhouette[Security.Env],
  val users: UserService,
  val uploads: Uploads,
  implicit val visits: VisitService,
  implicit val webjars: WebJarsUtil,
  implicit val ctx: ExecutionContext
) extends BaseOptAuthController(components, config, documents, users)
    with HasTEISnippets
    with HasVisitLogging
    with I18nSupport {
  
  def resolveFromAnnotation(uuid: UUID) = Action.async { implicit request =>
    annotations.findById(uuid).flatMap {
      case Some((annotation, _)) =>
        documents.findPartById(annotation.annotates.filepartId).map {
          case Some(part) =>
            Redirect(routes.AnnotationController.showAnnotationView(part.getDocumentId, part.getSequenceNo)
              .withFragment(annotation.annotationId.toString).toString).flashing("annotation" -> annotation.annotationId.toString)
            
          case None => InternalServerError // Annotation points to non-existing part? Should never happen
        }
        
      case None => Future.successful(NotFoundPage)
    }
  }
  
  def resolveFromPart(uuid: UUID) = Action.async { implicit request =>
    documents.findPartById(uuid).map {
      case Some(part) =>
        Redirect(routes.AnnotationController.showAnnotationView(part.getDocumentId, part.getSequenceNo))
        
      case None =>  NotFoundPage
    }
  }

  /** Shows the annotation view for a specific document part **/
  def showAnnotationView(documentId: String, seqNo: Int) = silhouette.UserAwareAction.async { implicit request =>
    val loggedIn = request.identity
    
    val fPreferences = documents.getPreferences(documentId)
    
    val fRedirectedVia = request.flash.get("annotation") match {
      case Some(annotationId) => annotations.findById(UUID.fromString(annotationId)).map { _.map(_._1) }
      case None => Future.successful(None)
    }
    
    def fResponse(prefs: Seq[DocumentPreferencesRecord], via: Option[Annotation]) = 
      documentPartResponse(documentId, seqNo, loggedIn, { case (doc, currentPart, accesslevel) =>
        if (accesslevel.canReadData)
          renderResponse(doc, currentPart, loggedIn, accesslevel, prefs, via.map(AnnotationSummary.from))
        else if (loggedIn.isEmpty) // No read rights - but user is not logged in yet
          Future.successful(Redirect(controllers.landing.routes.LoginLogoutController.showLoginForm(None)))
        else
          Future.successful(ForbiddenPage)
      })
    
    for {
      preferences <- fPreferences
      redirectedVia <- fRedirectedVia
      response <- fResponse(preferences, redirectedVia)
    } yield response
  }

  private def renderResponse(
    doc: ExtendedDocumentMetadata,
    currentPart: DocumentFilepartRecord,
    loggedInUser: Option[User],
    accesslevel: RuntimeAccessLevel,
    prefs: Seq[DocumentPreferencesRecord],
    redirectedVia: Option[AnnotationSummary]
  )(implicit request: RequestHeader) = {

    logDocumentView(doc.document, Some(currentPart), accesslevel)

    // Needed in any case - start now (val)
    val fCountAnnotations = annotations.countByDocId(doc.id)

    // Needed only for Text and TEI - start on demand (def)
    def fReadTextfile() = uploads.readTextfile(doc.ownerName, doc.id, currentPart.getFile)

    // Generic conditional: is the user authorized to see the content? Render 'forbidden' page if not.
    def ifAuthorized(result: Result, annotationCount: Long) =
      if (accesslevel.canReadAll) result else Ok(views.html.document.annotation.forbidden(doc, currentPart, loggedInUser, annotationCount))

    ContentType.withName(currentPart.getContentType) match {

      case Some(ContentType.IMAGE_UPLOAD) | Some(ContentType.IMAGE_IIIF) =>
        fCountAnnotations.map { c =>
          ifAuthorized(Ok(views.html.document.annotation.image(doc, currentPart, loggedInUser, accesslevel, prefs, c, redirectedVia)), c)
        }

      case Some(ContentType.TEXT_PLAIN) =>
        fReadTextfile() flatMap {
          case Some(content) =>
            fCountAnnotations.map { c =>
              ifAuthorized(Ok(views.html.document.annotation.text(doc, currentPart, loggedInUser, accesslevel, prefs, c, content, redirectedVia)), c)
            }

          case None =>
            // Filepart found in DB, but not file on filesystem
            Logger.error(s"Filepart recorded in the DB is missing on the filesystem: ${doc.ownerName}, ${doc.id}")
            Future.successful(InternalServerError)
        }

      case Some(ContentType.TEXT_TEIXML) =>
        fReadTextfile flatMap {
          case Some(content) =>
            fCountAnnotations.map { c =>
              val preview = previewFromTEI(content)
              ifAuthorized(Ok(views.html.document.annotation.tei(doc, currentPart, loggedInUser, accesslevel, prefs, preview, c, redirectedVia)), c)
            }

          case None =>
            // Filepart found in DB, but not file on filesystem
            Logger.error(s"Filepart recorded in the DB is missing on the filesystem: ${doc.ownerName}, ${doc.id}")
            Future.successful(InternalServerError)
        }

      case Some(ContentType.DATA_CSV) =>
        fCountAnnotations.map { c =>
          ifAuthorized(Ok(views.html.document.annotation.table(doc, currentPart, loggedInUser, accesslevel, prefs, c)), c)
        }

      case _ =>
        // Unknown content type in DB, or content type we don't have an annotation view for - should never happen
        Future.successful(InternalServerError)
    }

  }

}
