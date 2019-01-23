package controllers.api.annotation

import com.mohiva.play.silhouette.api.Silhouette
import controllers._
import controllers.api.annotation.stubs._
import java.io.File
import java.util.UUID
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import play.api.http.FileMimeTypes
import play.api.mvc.{AnyContent, ControllerComponents, Request, Result}
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.{ExecutionContext, Future}
import services.{ContentType, HasDate}
import services.annotation._
import services.annotation.relation._
import services.contribution._
import services.document.{DocumentService, DocumentInfo, RuntimeAccessLevel}
import services.user.UserService
import services.image.ImageService
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import storage.uploads.Uploads

@Singleton
class AnnotationAPIController @Inject() (
  val components: ControllerComponents,
  val annotationService: AnnotationService,
  val contributions: ContributionService,
  val documents: DocumentService,
  val silhouette: Silhouette[Security.Env],
  val users: UserService,
  implicit val config: Configuration,
  implicit val mimeTypes: FileMimeTypes,
  implicit val tmpFile: TemporaryFileCreator,
  implicit val uploads: Uploads,
  implicit val ctx: ExecutionContext
) extends BaseController(components, config, users)
    with HasAnnotationValidation
    with HasPrettyPrintJSON
    with HasTextSnippets
    with HasTEISnippets
    with HasCSVParsing {

  // Frontent serialization format
  import services.annotation.FrontendAnnotation._

  def listAnnotationsInDocument(docId: String) = listAnnotations(docId, None)

  def listAnnotationsInPart(docId: String, partNo: Int) = listAnnotations(docId, Some(partNo))

  private def listAnnotations(docId: String, partNo: Option[Int]) = silhouette.UserAwareAction.async { implicit request =>
    // TODO currently annotation read access is unrestricted - do we want that?
    (docId, partNo) match {
      case (id, Some(seqNo)) =>
        // Load annotations for specific doc part
        documents.findPartByDocAndSeqNo(id, seqNo).flatMap {
          case Some(filepart) =>
            annotationService.findByFilepartId(filepart.getId)
              .map { annotations =>
                // Join in places, if requested
                jsonOk(Json.toJson(annotations.map(_._1)))
              }

          case None =>
            Future.successful(NotFoundPage)
        }

      case (id, None) =>
        // Load annotations for entire doc
        annotationService.findByDocId(id).map(annotations => jsonOk(Json.toJson(annotations.map(_._1))))
    }
  }

  /** Common boilerplate code for all API methods carrying JSON config payload **/
  private def jsonOp[T](op: T => Future[Result])(implicit request: Request[AnyContent], reads: Reads[T]) = {
    request.body.asJson match {
      case Some(json) =>
        Json.fromJson[T](json) match {
          case s: JsSuccess[T]  => op(s.get)
          case e: JsError =>
            Logger.warn("Call to annotation API but invalid JSON: " + e.toString)
            Future.successful(BadRequest)
        }
      case None =>
        Logger.warn("Call to annotation API but no JSON payload")
        Future.successful(BadRequest)
    }
  }

  def createAnnotation() = silhouette.SecuredAction.async { implicit request => jsonOp[AnnotationStub] { annotationStub =>
    val username = request.identity.username

    // Fetch the associated document to check access privileges
    documents.getDocumentRecord(annotationStub.annotates.documentId, Some(username)).flatMap(_ match {
      case Some((document, accesslevel)) => {
        if (accesslevel.canWrite) {
          val annotation = annotationStub.toAnnotation(username)
          val f = for {
            (annotationStored, previousVersion) <- annotationService.upsertAnnotation(annotation)
            success <- if (annotationStored)
                         contributions.insertContributions(validateUpdate(annotation, previousVersion, document))
                       else
                         Future.successful(false)
          } yield success

          f.map(success => if (success) Ok(Json.toJson(annotation)) else InternalServerError)
        } else {
          // No write permissions
          Future.successful(Forbidden)
        }
      }

      case None =>
        Logger.warn("POST to /annotations but annotation points to unknown document: " + annotationStub.annotates.documentId)
        Future.successful(NotFound)
    })
  }}

  def bulkUpsert() = silhouette.SecuredAction.async { implicit request => jsonOp[Seq[AnnotationStub]] { annotationStubs =>
    // We currently restrict to bulk upserts for a single document part only
    val username = request.identity.username
    val documentIds = annotationStubs.map(_.annotates.documentId).distinct
    val partIds = annotationStubs.map(_.annotates.filepartId).distinct

    if (documentIds.size == 1 || partIds.size == 1) {
      documents.getExtendedInfo(documentIds.head, Some(username)).flatMap {
        case Some((doc, accesslevel)) =>
          if (accesslevel.canWrite) {
            doc.fileparts.find(_.getId == partIds.head) match {
              case Some(filepart) =>
                val annotations = annotationStubs.map(_.toAnnotation(username))
                annotationService.upsertAnnotations(annotations).map { failed =>
                  if (failed.size == 0)
                    // TODO add username and timestamp
                    jsonOk(Json.toJson(annotations))
                  else
                    InternalServerError
                }

              case None =>
                Logger.warn("Bulk upsert with invalid content: filepart not in document: " + documentIds.head + "/" + partIds.head)
                Future.successful(BadRequest)
            }
          } else {
            // No write permissions
            Future.successful(Forbidden)
          }

        case None =>
          Logger.warn("Bulk upsert request points to unknown document: " + documentIds.head)
          Future.successful(NotFound)
      }
    } else {
      Logger.warn("Bulk upsert request for multiple document parts")
      Future.successful(BadRequest)
    }
  }}

  def getImage(id: UUID) = silhouette.UserAwareAction.async { implicit request =>
    val username = request.identity.map(_.username)
    annotationService.findById(id).flatMap {
      case Some((annotation, _)) =>
        val docId = annotation.annotates.documentId
        val partId = annotation.annotates.filepartId

        documents.getExtendedInfo(docId, username).flatMap {
          case Some((doc, accesslevel)) =>
            doc.fileparts.find(_.getId == partId) match {
              case Some(filepart) =>
                if (accesslevel.canReadAll)
                  ContentType.withName(filepart.getContentType) match {
                    case Some(ContentType.IMAGE_UPLOAD) =>
                      ImageService.cutout(doc, filepart, annotation).map { file =>
                        Ok.sendFile(file)
                      }     
                      
                    case Some(ContentType.IMAGE_IIIF) =>
                      val snippetUrl = ImageService.iiifSnippet(doc, filepart, annotation)
                      Future.successful(Redirect(snippetUrl))
                      
                    case _ => // Should never happen
                      Future.successful(InternalServerError)
                  }
                else
                  Future.successful(Forbidden)

              case None =>
                Logger.error(s"Attempted to render image for annotation $id but filepart $partId not found")
                Future.successful(InternalServerError)
            }

          case None =>
            // Annotation exists, but not the doc?
            Logger.error(s"Attempted to render image for annotation $id but document $docId not found")
            Future.successful(InternalServerError)
        }

      case None => Future.successful(NotFound)
    }
  }

  def getAnnotation(id: UUID, includeContext: Boolean) = silhouette.UserAwareAction.async { implicit request =>
    def getTextContext(doc: DocumentInfo, part: DocumentFilepartRecord, annotation: Annotation): Future[JsValue] =
      uploads.readTextfile(doc.ownerName, doc.id, part.getFile) map {
        case Some(text) =>
          val snippet = ContentType.withName(part.getContentType).get match {
            case ContentType.TEXT_TEIXML => snippetFromTEI(text, annotation.anchor)
            case ContentType.TEXT_PLAIN => snippetFromText(text, annotation)
            case _ => throw new RuntimeException("Attempt to retrieve text snippet for non-text doc part - should never happen")
          }

          Json.obj("snippet" -> snippet.text, "char_offset" -> snippet.offset)

        case None =>
          Logger.warn("No text content found for filepart " + annotation.annotates.filepartId)
          JsNull
      }

    def getDataContext(doc: DocumentInfo, part: DocumentFilepartRecord, annotation: Annotation): Future[JsValue] = {
      uploads.getDocumentDir(doc.ownerName, doc.id).map { dir =>
        extractLine(new File(dir, part.getFile), annotation.anchor.substring(4).toInt).map(snippet => Json.toJson(snippet))
      } getOrElse {
        Logger.warn("No file content found for filepart " + annotation.annotates.filepartId)
        Future.successful(JsNull)
      }
    }

    def getContext(doc: DocumentInfo, annotation: Annotation): Future[JsValue] = annotation.annotates.contentType match {
      case t if t.isImage => Future.successful(JsNull)

      case t if t.isText | t.isData => doc.fileparts.find(_.getId == annotation.annotates.filepartId) match {
        case Some(part) =>
          if (t.isText) getTextContext(doc, part, annotation)
          else getDataContext(doc, part, annotation)

        case None =>
          // Annotation referenced a part ID that's not in the database
          Logger.error("Annotation points to filepart " + annotation.annotates.filepartId + " but not in DB")
          Future.successful(JsNull)
      }

      case _ =>
        Logger.error("Annotation indicates unsupported content type " + annotation.annotates.contentType)
        Future.successful(JsNull)
    }

    annotationService.findById(id).flatMap {
      case Some((annotation, _)) => {
        if (includeContext) {
          documents.getExtendedInfo(annotation.annotates.documentId, request.identity.map(_.username)).flatMap {
            case Some((doc, accesslevel)) =>
              if (accesslevel.canReadData)
                getContext(doc, annotation).map(context =>
                  jsonOk(Json.toJson(annotation).as[JsObject] ++ Json.obj("context" -> context)))
              else
                Future.successful(ForbiddenPage)

            case _ =>
              Logger.warn("Annotation points to document " + annotation.annotates.documentId + " but not in DB")
              Future.successful(NotFoundPage)
          }
        } else {
          Future.successful(jsonOk(Json.toJson(annotation)))
        }
      }

      case None => Future.successful(NotFoundPage)
    }
  }

  private def createDeleteContribution(annotation: Annotation, document: DocumentRecord, user: String, time: DateTime) =
    Contribution(
      ContributionAction.DELETE_ANNOTATION,
      user,
      time,
      Item(
        ItemType.ANNOTATION,
        annotation.annotates.documentId,
        document.getOwner,
        Some(annotation.annotates.filepartId),
        Some(annotation.annotates.contentType),
        Some(annotation.annotationId),
        None, None, None
      ),
      annotation.contributors,
      getContext(annotation)
    )

  def deleteAnnotation(id: UUID) = silhouette.SecuredAction.async { implicit request =>
    val username = request.identity.username
    annotationService.findById(id).flatMap {
      case Some((annotation, version)) =>
        // Fetch the associated document
        documents.getDocumentRecord(annotation.annotates.documentId, Some(username)).flatMap {
          case Some((document, accesslevel)) => {
            if (accesslevel.canWrite) {
              val now = DateTime.now
              annotationService.deleteAnnotation(id, username, now).flatMap {
                case Some(annotation) =>
                  contributions.insertContribution(createDeleteContribution(annotation, document, username, now)).map(success =>
                    if (success) Status(200) else InternalServerError)

                case None =>
                  Future.successful(NotFoundPage)
              }
            } else {
              Future.successful(ForbiddenPage)
            }
          }

          case None => {
            // Annotation on a non-existing document? Can't happen except DB integrity is broken
            Logger.warn(s"Annotation points to document ${annotation.annotates.documentId} but not in DB")
            Future.successful(InternalServerError)
          }
        }

      case None => Future.successful(NotFoundPage)
    }
  }
  
  def bulkDelete() = silhouette.SecuredAction.async { implicit request => jsonOp[Seq[UUID]] { ids =>
    val username = request.identity.username
    val now = DateTime.now
    
    // Shorthand for readability
    def getDocumentIds(affectedAnnotations: Seq[Option[(Annotation, Long)]]) =
      affectedAnnotations.flatten.map(_._1.annotates.documentId)
      
    // Deletes one annotation after checking access permissions
    def deleteOne(annotation: Annotation, docsAndPermissions: Seq[(DocumentInfo, RuntimeAccessLevel)]) = {
      val hasWritePermissions = docsAndPermissions
        .find(_._1.id == annotation.annotates.documentId)
        .map(_._2.canWrite)
        .getOrElse(false)
        
      if (hasWritePermissions) {
        annotationService
          .deleteAnnotation(annotation.annotationId, username, now)
          .map(_ => true) // True if delete is successful
          .recover { case t: Throwable =>
            t.printStackTrace()
            Logger.error(s"Something went wrong while batch-deleting annotation ${annotation.annotationId}")
            Logger.error(t.toString)
            false // False in case stuff breaks 
          } 
      } else {
        Logger.warn(s"Prevented malicious batch-delete attempt by user ${username}")
        Future.successful(false)
      }
    }
    
    // Fetch affected annotations and documents
    val f = for {
      affectedAnnotations <- Future.sequence(ids.map(annotationService.findById))
      affectedDocuments <- Future.sequence { 
        getDocumentIds(affectedAnnotations).map { docId => 
          documents.getExtendedInfo(docId, Some(username))
        }
      }
    } yield (affectedAnnotations.flatten.map(_._1), affectedDocuments.flatten)
    
    // Bulk delete loop
    val fBulkDelete = f.flatMap { case (annotations, docsAndPermissions) =>
      // Delete annotations one by one, keeping a list of those that failed
      annotations.foldLeft(Future.successful(Seq.empty[Annotation])) { case (f, next) =>
        f.flatMap { failed =>          
          deleteOne(next, docsAndPermissions).map { success =>
            if (success) failed else failed :+ next
          }
        }
      }
    } map { failed =>
      if (!failed.isEmpty)
        Logger.error(s"Failed to bulk-delete the following annotations: ${failed.map(_.annotationId).mkString}") 
      failed.isEmpty
    }
    
    fBulkDelete.map(success => if (success) Ok else InternalServerError)
  }}

}
