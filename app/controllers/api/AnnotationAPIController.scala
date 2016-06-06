package controllers.api

import controllers.BaseController
import java.util.UUID
import javax.inject.Inject
import models.{ ContentType, HasDate }
import models.annotation._
import models.contribution._
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

  def getAnnotationsForDocument(docId: String) = getAnnotations(docId, None)
    
  def getAnnotationsForPart(docId: String, partNo: Int) = getAnnotations(docId, Some(partNo))

  /** TODO currently annotation read access is unlimited to any logged in user - do we want that? **/
  def getAnnotations(docId: String, partNo: Option[Int]) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    (docId, partNo) match {
      case (id, Some(seqNo)) =>
        // Load annotations for specific doc part
        DocumentService.findPartByDocAndSeqNo(id, seqNo).flatMap(_ match {
          case Some(filepart) =>
            AnnotationService.findByFilepartId(filepart.getId)
              .map{ annotations =>
                // Join in places, if requested
                Ok(Json.toJson(annotations.map(_._1)))
              }

          case None =>
            Future.successful(NotFound)
        })

      case (id, None) =>
        // Load annotations for entire doc
        AnnotationService.findByDocId(id).map(annotations => Ok(Json.toJson(annotations.map(_._1))))
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

            // TODO wait for response!
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
  
  def deleteAnnotation(id: UUID) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    AnnotationService.findById(id).flatMap(_ match {
      case Some((annotation, version)) => {
        // Fetch the associated document
        DocumentService.findById(annotation.annotates.documentId).flatMap(_ match {
          case Some(document) => {
            // TODO check if the user has write permissions
            // TODO for now we'll just check ownership
            if (document.getOwner == loggedIn.user.getUsername) {
              // If so, are there any comment nodes left that are *not* by this user?
              AnnotationService.deleteAnnotation(id).map(success => {
                if (success)
                  Status(204)
                else
                  InternalServerError
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
      
      case None => Future.successful(NotFound)
    })
  }

}
