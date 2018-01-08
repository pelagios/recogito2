package controllers.api

import controllers._
import java.io.File
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.OptionalAuthElement
import models.{ ContentType, HasDate }
import models.annotation._
import models.contribution._
import models.document.{ DocumentService, DocumentInfo }
import models.user.UserService
import models.image.ImageService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.joda.time.DateTime
import play.api.{ Configuration, Logger }
import play.api.http.FileMimeTypes
import play.api.mvc.{ AnyContent, Request, Result }
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.{ ExecutionContext, Future }
import storage.Uploads

/** Encapsulates those parts of an annotation that are submitted from the client **/
case class AnnotationStub(

  annotationId: Option[UUID],

  annotates: AnnotatedObject,

  anchor: String,

  bodies: Seq[AnnotationBodyStub])

object AnnotationStub {

  implicit val annotationStubReads: Reads[AnnotationStub] = (
    (JsPath \ "annotation_id").readNullable[UUID] and
    (JsPath \ "annotates").read[AnnotatedObject] and
    (JsPath \ "anchor").read[String] and
    (JsPath \ "bodies").read[Seq[AnnotationBodyStub]]
  )(AnnotationStub.apply _)

}

/** Partial annotation body **/
case class AnnotationBodyStub(

  hasType: AnnotationBody.Type,

  lastModifiedBy: Option[String],

  lastModifiedAt: Option[DateTime],

  value: Option[String],

  uri: Option[String],
  
  note: Option[String],

  status: Option[AnnotationStatusStub])

object AnnotationBodyStub extends HasDate {

  implicit val annotationBodyStubReads: Reads[AnnotationBodyStub] = (
    (JsPath \ "type").read[AnnotationBody.Value] and
    (JsPath \ "last_modified_by").readNullable[String] and
    (JsPath \ "last_modified_at").readNullable[DateTime] and
    (JsPath \ "value").readNullable[String] and
    (JsPath \ "uri").readNullable[String] and
    (JsPath \ "note").readNullable[String] and
    (JsPath \ "status").readNullable[AnnotationStatusStub]
  )(AnnotationBodyStub.apply _)

}

/** Partial annotation status **/
case class AnnotationStatusStub(

  value: AnnotationStatus.Value,

  setBy: Option[String],

  setAt: Option[DateTime]

)

object AnnotationStatusStub extends HasDate {

  implicit val annotationStatusStubReads: Reads[AnnotationStatusStub] = (
    (JsPath \ "value").read[AnnotationStatus.Value] and
    (JsPath \ "set_by").readNullable[String] and
    (JsPath \ "set_at").readNullable[DateTime]
  )(AnnotationStatusStub.apply _)

}

@Singleton
class AnnotationAPIController @Inject() (
    val config: Configuration,
    val annotationService: AnnotationService,
    val contributions: ContributionService,
    val documents: DocumentService,
    val users: UserService,
    implicit val mimeTypes: FileMimeTypes,
    implicit val tmpFile: TemporaryFileCreator,
    implicit val uploads: Uploads,
    implicit val ctx: ExecutionContext
  ) extends BaseController(config, users)
      with OptionalAuthElement
      with HasAnnotationValidation
      with HasPrettyPrintJSON
      with HasTextSnippets 
      with HasTEISnippets
      with HasCSVParsing {

  def listAnnotationsInDocument(docId: String) = listAnnotations(docId, None)

  def listAnnotationsInPart(docId: String, partNo: Int) = listAnnotations(docId, Some(partNo))

  private def listAnnotations(docId: String, partNo: Option[Int]) = AsyncStack { implicit request =>
    // TODO currently annotation read access is unlimited to any logged in user - do we want that?
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

  private def stubToAnnotation(stub: AnnotationStub, user: String) = {
    val now = DateTime.now()
    Annotation(
      stub.annotationId.getOrElse(UUID.randomUUID),
      UUID.randomUUID,
      stub.annotates,
      Seq(user),
      stub.anchor,
      Some(user),
      now,
      stub.bodies.map(b => AnnotationBody(
        b.hasType,
        Some(b.lastModifiedBy.getOrElse(user)),
        b.lastModifiedAt.getOrElse(now),
        b.value,
        b.uri,
        b.note,
        b.status.map(s =>
          AnnotationStatus(
            s.value,
            Some(s.setBy.getOrElse(user)),
            s.setAt.getOrElse(now))))))
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

  def createAnnotation() = AsyncStack { implicit request => jsonOp[AnnotationStub] { annotationStub =>
    // Fetch the associated document to check access privileges
    documents.getDocumentRecord(annotationStub.annotates.documentId, loggedIn.map(_.username)).flatMap(_ match {
      case Some((document, accesslevel)) => {
        if (accesslevel.canWrite) {
          val annotation = stubToAnnotation(annotationStub, loggedIn.map(_.username).getOrElse("guest"))
          val f = for {
            (annotationStored, _, previousVersion) <- annotationService.insertOrUpdateAnnotation(annotation)
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
  
  def bulkUpsert() = AsyncStack { implicit request => jsonOp[Seq[AnnotationStub]] { annotationStubs =>
    // We currently restrict to bulk upserts for a single document part only
    val username = loggedIn.map(_.username)
    val documentIds = annotationStubs.map(_.annotates.documentId).distinct
    val partIds = annotationStubs.map(_.annotates.filepartId).distinct    
    
    if (documentIds.size == 1 || partIds.size == 1) {
      documents.getExtendedInfo(documentIds.head, username).flatMap {
        case Some((doc, accesslevel)) =>
          if (accesslevel.canWrite) {
            doc.fileparts.find(_.getId == partIds.head) match {
              case Some(filepart) =>
                val annotations = annotationStubs.map(stub => stubToAnnotation(stub, username.getOrElse("guest")))
                annotationService.insertOrUpdateAnnotations(annotations).map { failed =>
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

  def getImage(id: UUID) = AsyncStack { implicit request =>    
    val username = loggedIn.map(_.username)
    
    annotationService.findById(id).flatMap {
      case Some((annotation, _)) =>
        val docId = annotation.annotates.documentId
        val partId = annotation.annotates.filepartId
        
        documents.getExtendedInfo(docId, username).flatMap {
          case Some((doc, accesslevel)) =>
            doc.fileparts.find(_.getId == partId) match {
              case Some(filepart) =>
                if (accesslevel.canRead) ImageService.cutout(doc, filepart, annotation).map(file =>
                  Ok.sendFile(file))
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

  def getAnnotation(id: UUID, includeContext: Boolean) = AsyncStack { implicit request =>
    
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
          documents.getExtendedInfo(annotation.annotates.documentId, loggedIn.map(_.username)).flatMap {
            case Some((doc, accesslevel)) =>
              if (accesslevel.canRead)
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

  def deleteAnnotation(id: UUID) = AsyncStack { implicit request =>
    annotationService.findById(id).flatMap {
      case Some((annotation, version)) =>
        // Fetch the associated document
        documents.getDocumentRecord(annotation.annotates.documentId, loggedIn.map(_.username)).flatMap {
          case Some((document, accesslevel)) => {
            if (accesslevel.canWrite) {
              val user = loggedIn.map(_.username).getOrElse("guest")
              val now = DateTime.now
              annotationService.deleteAnnotation(id, user, now).flatMap {
                case Some(annotation) =>
                  contributions.insertContribution(createDeleteContribution(annotation, document, user, now)).map(success =>
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
            Logger.warn("Annotation points to document " + annotation.annotates.documentId + " but not in DB")
            Future.successful(InternalServerError)
          }
        }

      case None => Future.successful(NotFoundPage)
    }
  }

}
