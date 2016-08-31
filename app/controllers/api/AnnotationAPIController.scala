package controllers.api

import controllers._
import java.util.UUID
import javax.inject.Inject
import jp.t2v.lab.play2.auth.OptionalAuthElement
import models.{ ContentType, HasDate }
import models.annotation._
import models.contribution._
import models.document.{ DocumentService, DocumentInfo }
import models.user.UserService
import org.joda.time.DateTime
import play.api.{ Configuration, Logger }
import play.api.mvc.{ AnyContent, Request, Result }
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

  status: Option[AnnotationStatusStub])

object AnnotationBodyStub extends HasDate {

  implicit val annotationBodyStubReads: Reads[AnnotationBodyStub] = (
    (JsPath \ "type").read[AnnotationBody.Value] and
    (JsPath \ "last_modified_by").readNullable[String] and
    (JsPath \ "last_modified_at").readNullable[DateTime] and
    (JsPath \ "value").readNullable[String] and
    (JsPath \ "uri").readNullable[String] and
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

class AnnotationAPIController @Inject() (
    val config: Configuration,
    val annotations: AnnotationService,
    val contributions: ContributionService,
    val documents: DocumentService,
    val users: UserService,
    val uploads: Uploads,
    implicit val ctx: ExecutionContext
  ) extends BaseController(config, users)
      with OptionalAuthElement
      with HasAnnotationValidation
      with HasPrettyPrintJSON
      with HasTextSnippets {

  def listAnnotationsInDocument(docId: String) = listAnnotations(docId, None)

  def listAnnotationsInPart(docId: String, partNo: Int) = listAnnotations(docId, Some(partNo))

  private def listAnnotations(docId: String, partNo: Option[Int]) = AsyncStack { implicit request =>
    // TODO currently annotation read access is unlimited to any logged in user - do we want that?
    (docId, partNo) match {
      case (id, Some(seqNo)) =>
        // Load annotations for specific doc part
        documents.findPartByDocAndSeqNo(id, seqNo).flatMap {
          case Some(filepart) =>
            annotations.findByFilepartId(filepart.getId)
              .map { annotations =>
                // Join in places, if requested
                jsonOk(Json.toJson(annotations.map(_._1)))
              }

          case None =>
            Future.successful(NotFoundPage)
        }

      case (id, None) =>
        // Load annotations for entire doc
        annotations.findByDocId(id).map(annotations => jsonOk(Json.toJson(annotations.map(_._1))))
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
        b.status.map(s =>
          AnnotationStatus(
            s.value,
            Some(s.setBy.getOrElse(user)),
            s.setAt.getOrElse(now))))))
  }

  def createAnnotation() = AsyncStack { implicit request =>
    request.body.asJson match {
      case Some(json) =>
        Json.fromJson[AnnotationStub](json) match {
          case s: JsSuccess[AnnotationStub] => {
            // Fetch the associated document to check access privileges
            documents.getDocumentRecord(s.get.annotates.documentId, loggedIn.map(_.user.getUsername)).flatMap(_ match {
              case Some((document, accesslevel)) => {
                if (accesslevel.canWrite) {
                  val annotation = stubToAnnotation(s.get, loggedIn.map(_.user.getUsername).getOrElse("guest"))
                  val f = for {
                    (annotationStored, _, previousVersion) <- annotations.insertOrUpdateAnnotation(annotation)
                    success <- if (annotationStored)
                                 contributions.insertContributions(validateUpdate(annotation, previousVersion))
                               else
                                 Future.successful(false)
                  } yield success

                  f.map(success => if (success) Ok(Json.toJson(annotation)) else InternalServerError)
                } else {
                  // No write permissions
                  Future.successful(ForbiddenPage)
                }
              }

              case None =>
                Logger.warn("POST to /annotations but annotation points to unknown document: " + s.get.annotates.documentId)
                Future.successful(NotFoundPage)
            })
          }

          case e: JsError =>
            Logger.warn("POST to /annotations but invalid JSON: " + e.toString)
            Future.successful(BadRequest)
        }

      case None =>
        Logger.warn("POST to /annotations but no JSON payload")
        Future.successful(BadRequest)

    }
  }

  private def getTextAnnotationWithContext(doc: DocumentInfo, annotation: Annotation)(implicit request: Request[AnyContent]) = {
    doc.fileparts.find(_.getId == annotation.annotates.filepartId) match {
      case Some(part) => uploads.readTextfile(doc.ownerName, doc.id, part.getFilename) match {
          case Some(text) =>
            val snippet = extractTextSnippet(text, annotation)
            jsonOk(Json.toJson(annotation).as[JsObject] ++ Json.obj("context" -> Json.obj("snippet" -> snippet.text, "char_offset" -> snippet.offset)))

          case None =>
            Logger.warn("No text content found for filepart " + annotation.annotates.filepartId)
            InternalServerError
        }

      case None =>
        // Annotation referenced a part ID that's not in the database
        Logger.error("Annotation points to filepart " + annotation.annotates.filepartId + " but not in DB")
        InternalServerError
      }
  }

  /** This may get more sophisticated in the future and include a URL to a clipped and rotated image snippet **/ 
  private def getImageAnnotationWithContext(doc: DocumentInfo, annotation: Annotation)(implicit request: Request[AnyContent]) = {
    jsonOk(Json.toJson(annotation))
  }

  def getAnnotation(id: UUID, includeContext: Boolean) = AsyncStack { implicit request =>
    annotations.findById(id).flatMap {
      case Some((annotation, _)) => {
        
        if (includeContext) {
          documents.getExtendedInfo(annotation.annotates.documentId, loggedIn.map(_.user.getUsername)).map(_ match {
            case Some((doc, accesslevel)) =>
              if (accesslevel.canRead) {
                val contentType = annotation.annotates.contentType
                if (contentType.isText) {
                  getTextAnnotationWithContext(doc, annotation)
                } else if (contentType.isImage) {
                  getImageAnnotationWithContext(doc, annotation)
                } else {
                  Logger.error("Annotation indicates unsupported content type " + annotation.annotates.contentType)
                  InternalServerError
                }
              } else {
                ForbiddenPage
              }

            case _ =>
              Logger.warn("Annotation points to document " + annotation.annotates.documentId + " but not in DB")
              NotFoundPage
          })
        } else {
          Future.successful(jsonOk(Json.toJson(annotation)))
        }
      }

      case None => Future.successful(NotFoundPage)
    }
  }

  private def createDeleteContribution(annotation: Annotation, user: String, time: DateTime) =
    Contribution(
      ContributionAction.DELETE_ANNOTATION,
      user,
      time,
      Item(
        ItemType.ANNOTATION,
        annotation.annotates.documentId,
        Some(annotation.annotates.filepartId),
        annotation.annotates.contentType,
        Some(annotation.annotationId),
        None, None, None
      ),
      annotation.contributors,
      getContext(annotation)
    )

  def deleteAnnotation(id: UUID) = AsyncStack { implicit request =>
    annotations.findById(id).flatMap {
      case Some((annotation, version)) =>
        // Fetch the associated document
        documents.getDocumentRecord(annotation.annotates.documentId, loggedIn.map(_.user.getUsername)).flatMap {
          case Some((document, accesslevel)) => {
            if (accesslevel.canWrite) {
              val user = loggedIn.map(_.user.getUsername).getOrElse("guest")
              val now = DateTime.now
              annotations.deleteAnnotation(id, user, now).flatMap {
                case Some(annotation) =>
                  contributions.insertContribution(createDeleteContribution(annotation, user, now)).map(success =>
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
