package controllers.api

import controllers._
import java.util.UUID
import javax.inject.Inject
import jp.t2v.lab.play2.auth.OptionalAuthElement
import models.HasDate
import models.annotation._
import models.contribution._
import models.document.DocumentService
import org.joda.time.DateTime
import play.api.Logger
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.mvc.Controller
import scala.concurrent.Future
import storage.{ DB, FileAccess }
import play.api.libs.json.JsObject

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

class AnnotationAPIController @Inject() (implicit val cache: CacheApi, val db: DB)
  extends Controller with HasCache with HasDatabase with OptionalAuthElement with Security
                     with HasAnnotationValidation with HasPrettyPrintJSON with FileAccess with HasTextSnippets {

  def listAnnotationsInDocument(docId: String) = listAnnotations(docId, None)

  def listAnnotationsInPart(docId: String, partNo: Int) = listAnnotations(docId, Some(partNo))

  private def listAnnotations(docId: String, partNo: Option[Int]) = AsyncStack { implicit request =>
    // TODO currently annotation read access is unlimited to any logged in user - do we want that?
    (docId, partNo) match {
      case (id, Some(seqNo)) =>
        // Load annotations for specific doc part
        DocumentService.findPartByDocAndSeqNo(id, seqNo).flatMap(_ match {
          case Some(filepart) =>
            AnnotationService.findByFilepartId(filepart.getId)
              .map{ annotations =>
                // Join in places, if requested
                jsonOk(Json.toJson(annotations.map(_._1)))
              }

          case None =>
            Future.successful(NotFound(views.html.error404()))
        })

      case (id, None) =>
        // Load annotations for entire doc
        AnnotationService.findByDocId(id).map(annotations => jsonOk(Json.toJson(annotations.map(_._1))))
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
      case Some(json) => {
        Json.fromJson[AnnotationStub](json) match {
          case s: JsSuccess[AnnotationStub] => {
            // Fetch the associated document to check access privileges
            DocumentService.findById(s.get.annotates.documentId, loggedIn.map(_.user.getUsername)).flatMap(_ match {
              case Some((document, accesslevel)) => {
                if (accesslevel.canWrite) {
                  val annotation = stubToAnnotation(s.get, loggedIn.map(_.user.getUsername).getOrElse("guest"))
                  val f = for {
                    (annotationStored, _, previousVersion) <- AnnotationService.insertOrUpdateAnnotation(annotation)
                    success <- if (annotationStored)
                                 ContributionService.insertContributions(validateUpdate(annotation, previousVersion))
                               else
                                 Future.successful(false)
                  } yield success

                  f.map(success => if (success) Ok(Json.toJson(annotation)) else InternalServerError)
                } else {
                  // No write permissions
                  Future.successful(Forbidden)
                }
              }

              case None => {
                Logger.warn("POST to /annotations but annotation points to unknown document: " + s.get.annotates.documentId)
                Future.successful(NotFound(views.html.error404()))
              }
            })
          }

          case e: JsError => {
            Logger.warn("POST to /annotations but invalid JSON: " + e.toString)
            Future.successful(BadRequest)
          }
        }
      }

      case None => {
        Logger.warn("POST to /annotations but no JSON payload")
        Future.successful(BadRequest)
      }

    }
  }

  def getAnnotation(id: UUID, includeContext: Boolean) = AsyncStack { implicit request =>
    AnnotationService.findById(id).flatMap(_ match {
      case Some((annotation, _)) => {
        if (includeContext) {
          // Fetch the document, so we know the owner...
          DocumentService.findByIdWithFileparts(annotation.annotates.documentId, loggedIn.map(_.user.getUsername)).map(_ match {

            case Some((document, parts, accesslevel)) =>
              if (accesslevel.canRead) {
                // ...then fetch the content
                parts.filter(_.getId == annotation.annotates.filepartId).headOption match {
                  case Some(filepart) => readTextfile(document.getOwner, document.getId, filepart.getFilename) match {
                    case Some(text) => {
                      val snippet = extractTextSnippet(text, annotation)
                      jsonOk(Json.toJson(annotation).as[JsObject] ++ Json.obj("context" -> Json.obj("snippet" -> snippet.text, "char_offset" -> snippet.offset)))
                    }

                    case None => {
                      Logger.warn("No text content found for filepart " + annotation.annotates.filepartId)
                      InternalServerError
                    }
                  }

                  case None => {
                    // Annotation referenced a part ID that's not in the database
                    Logger.warn("Annotation points to filepart " + annotation.annotates.filepartId + " but not in DB")
                    InternalServerError
                  }
                }
              } else {
                // No read permission on this document
                Forbidden
              }

            case None => {
              Logger.warn("Annotation points to document " + annotation.annotates.documentId + " but not in DB")
              NotFound(views.html.error404())
            }
          })
        } else {
          Future.successful(jsonOk(Json.toJson(annotation)))
        }
      }

      case None => Future.successful(NotFound(views.html.error404()))
    })
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
    AnnotationService.findById(id).flatMap(_ match {
      case Some((annotation, version)) => {
        // Fetch the associated document
        DocumentService.findById(annotation.annotates.documentId, loggedIn.map(_.user.getUsername)).flatMap(_ match {
          case Some((document, accesslevel)) => {
            if (accesslevel.canWrite) {
              val user = loggedIn.map(_.user.getUsername).getOrElse("guest")
              val now = DateTime.now
              AnnotationService.deleteAnnotation(id, user, now).flatMap(_ match {
                case Some(annotation) =>
                  ContributionService.insertContribution(createDeleteContribution(annotation, user, now)).map(success =>
                    if (success) Status(200) else InternalServerError)
    
                case None =>
                  Future.successful(NotFound(views.html.error404()))
              })
            } else {
              Future.successful(Forbidden)
            }
          }

          case None => {
            // Annotation on a non-existing document? Can't happen except DB integrity is broken
            Logger.warn("Annotation points to document " + annotation.annotates.documentId + " but not in DB")
            Future.successful(InternalServerError)
          }
        })
      }

      case None => Future.successful(NotFound(views.html.error404()))
    })
  }

}
