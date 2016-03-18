package controllers.api

import controllers.BaseController
import java.util.UUID
import javax.inject.Inject
import models.annotation._
import models.content.DocumentService
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
case class AnnotationBodyStub(hasType: AnnotationBody.Type, value: Option[String], uri: Option[String])

object AnnotationBodyStub {

  implicit val annotationBodyStubFormat: Reads[AnnotationBodyStub] = (
    (JsPath \ "type").read[AnnotationBody.Value] and
    (JsPath \ "value").readNullable[String] and
    (JsPath \ "uri").readNullable[String]
  )(AnnotationBodyStub.apply _)

}

case class AnnotationStub(annotates: AnnotatedObject, anchor: String, bodies: Seq[AnnotationBodyStub])

object AnnotationStub {

  implicit val annotationStubFormat: Reads[AnnotationStub] = (
    (JsPath \ "annotates").read[AnnotatedObject] and
    (JsPath \ "anchor").read[String] and
    (JsPath \ "bodies").read[Seq[AnnotationBodyStub]]
  )(AnnotationStub.apply _)

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
              .map(annotations => Ok(Json.toJson(annotations)))

          case None =>
            Future.successful(NotFound)
        })

      case (Some(id), None) =>
        // Load annotations for entire doc
        AnnotationService.findByDocId(id).map(annotations => Ok(Json.toJson(annotations)))

      case _ =>
        // No doc ID
        Future.successful(BadRequest)

    }


  }

  /** TODO currently annotation creation is unlimited to any logged in user - need to check access rights! **/
  def createAnnotation() = StackAction(AuthorityKey -> Normal) { implicit request =>
    request.body.asJson match {

      case Some(json) => {
        Json.fromJson[AnnotationStub](json) match {
          case s: JsSuccess[AnnotationStub] => {
            val now = DateTime.now()
            val user = loggedIn.getUsername
            val annotation =
              Annotation(
                UUID.randomUUID,
                UUID.randomUUID,
                s.get.annotates,
                None,
                Seq(user),
                s.get.anchor,
                Some(user),
                now,
                s.get.bodies.map(b => AnnotationBody(b.hasType, Some(user), now, b.value, b.uri)),
                AnnotationStatus(AnnotationStatus.UNVERIFIED, Some(user), now))

            // TODO error reporting?
            AnnotationService.insertAnnotations(Seq(annotation))
            Ok(Json.toJson(annotation))
          }

          case e: JsError => {
            Logger.warn("POST to /annotations but invalid JSON")
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
