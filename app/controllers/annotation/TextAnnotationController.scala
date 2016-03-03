package controllers.annotation

import controllers.{ AbstractController, Security }
import java.util.UUID
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.Roles._
import models.{ Annotation, AnnotationService, AnnotationStatus, AnnotationBody, AnnotatedObject, DocumentService }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import storage.{ DB, FileAccess }
import scala.concurrent.Future
import play.api.Logger
import play.api.mvc.Action
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.joda.time.DateTime


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



class TextAnnotationController @Inject() (implicit val db: DB) extends AbstractController with AuthElement with Security with FileAccess {
    
  /** TODO temporary dummy implementation **/
  def showTextAnnotationView(documentId: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    val username = loggedIn.getUsername
    
    DocumentService.findByIdWithFileparts(documentId).map(_ match {
      case Some((document, fileparts)) => {
        // Verify if the user is allowed to access this document - TODO what about shared content?
        if (document.getOwner == username) {
          loadTextfile(username, fileparts.head.getFilename) match {
            case Some(content) =>
              Ok(views.html.annotation.text_annotation(document.getId, fileparts.head.getId, content))
            
            case None => {
              // Filepart found in DB, but no file on filesystem
              InternalServerError
            }
          }
        } else {
          Forbidden
        }
      }
        
      case None =>
        // No document with that ID found in DB
        NotFound
    })
  }
  
  def getAnnotationsFor(filepartId: Int) = Action.async { request =>
    AnnotationService.findByFilepartId(filepartId)
      .map(annotations => Ok(Json.toJson(annotations)))
  }
  
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
                s.get.bodies.map(b => 
                  AnnotationBody(b.hasType, Some(user), now, b.value, b.uri)),
                AnnotationStatus(AnnotationStatus.UNVERIFIED, Some(user), now))
                
            // TODO error reporting?
            AnnotationService.insertAnnotations(Seq(annotation))
            Ok("ok.")
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