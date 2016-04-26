package controllers.api

import controllers.BaseController
import java.util.UUID
import javax.inject.Inject
import models.HasDate
import models.annotation._
import models.document.DocumentService
import models.user.Roles._
import org.joda.time.DateTime
import play.api.Logger
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.Future
import storage.DB

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


class AnnotationAPIController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController {

  private val PARAM_DOC = "doc"

  private val PARAM_PART = "part"

  /** TODO currently annotation read access is unlimited to any logged in user - do we want that? **/
  def loadAnnotations() = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    val docId = getQueryParam(PARAM_DOC)
    val partNo = getQueryParam(PARAM_PART).map(_.toInt)

    (docId, partNo) match {

      case (Some(id), Some(seqNo)) =>
        // Load annotations for specific doc part
        DocumentService.findPartByDocAndSeqNo(id, seqNo).flatMap(_ match {
          case Some(filepart) =>
            AnnotationService.findByFilepartId(filepart.getId)
              .map(annotations => Ok(Json.toJson(annotations.map(_._1))))

          case None =>
            Future.successful(NotFound)
        })

      case (Some(id), None) =>
        // Load annotations for entire doc
        AnnotationService.findByDocId(id).map(annotations => Ok(Json.toJson(annotations.map(_._1))))

      case _ =>
        // No doc ID
        Future.successful(BadRequest)

    }


  }

  /** TODO currently annotation creation is unlimited to any logged in user - need to check access rights! **/
  def createAnnotation() = StackAction(AuthorityKey -> Normal) { implicit request =>
    request.body.asJson match {

      // TODO createdAt/By info for existing bodies is taken from the JSON, without
      // verifying against data stored on the server, i.e. potentially hackable
      // should we build in protection against this?
      
      case Some(json) => {
        Json.fromJson[AnnotationStub](json) match {
          case s: JsSuccess[AnnotationStub] => {
            val now = DateTime.now()
            val user = loggedIn.user.getUsername
            val annotation =
              Annotation(
                s.get.annotationId.getOrElse(UUID.randomUUID),
                UUID.randomUUID,
                s.get.annotates,
                None,
                Seq(user),
                s.get.anchor,
                Some(user),
                now,
                s.get.bodies.map(b => AnnotationBody(
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

            // TODO error reporting?
            AnnotationService.insertOrUpdateAnnotation(annotation)
            Ok(Json.toJson(annotation))
          }

          case e: JsError => {
            Logger.warn("POST to /annotations but invalid JSON: " + e.toString)
            BadRequest
          }
        }
      }

      case None => {
        Logger.warn("POST to /annotations but no JSON payload")
        BadRequest
      }

    }
  }

}
