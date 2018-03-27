package controllers.api

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, HasPrettyPrintJSON, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import services.document.{DocumentInfo, DocumentService}
import services.task.{TaskType, TaskService, TaskRecordAggregate}
import services.user.UserService
import services.user.Roles._
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.mvc.ControllerComponents 
import scala.concurrent.{ExecutionContext, Future}
import transform.georesolution.GeoresolutionService

case class TaskDefinition(
  taskType  : TaskType,
  documentId: String,
  filepartId: Option[UUID],
  args      : Map[String, String])

object TaskDefinition {

  implicit val taskDefinitonReads: Reads[TaskDefinition] = (
    (JsPath \ "task_type").read[String].map(str => TaskType(str)) and
    (JsPath \ "document_id").read[String] and
    (JsPath \ "filepart_id").readNullable[UUID] and
    (JsPath \ "args").read[Map[String, String]]
  )(TaskDefinition.apply _)

}

@Singleton
class TaskAPIController @Inject() (
    val components: ControllerComponents,
    val config: Configuration,
    val documents: DocumentService,
    val users: UserService,
    val georesolution: GeoresolutionService,
    val silhouette: Silhouette[Security.Env],
    val tasks: TaskService,
    implicit val system: ActorSystem,
    implicit val ctx: ExecutionContext
  ) extends BaseAuthController(components, config, documents, users) with HasPrettyPrintJSON {

  def spawnTask = silhouette.SecuredAction.async { implicit request =>
    request.body.asJson.map(json => Json.fromJson[TaskDefinition](json)) match {
      case Some(result) if result.isSuccess =>
        val taskDefinition = result.get
        documentResponse(taskDefinition.documentId, request.identity, { case (docInfo, accesslevel) =>
          if (accesslevel.canWrite)
            taskDefinition.taskType match {  
              case TaskType("GEORESOLUTION") =>
                georesolution.spawnTask(docInfo.document, docInfo.fileparts, taskDefinition.args)
                Ok
                
              case t =>
                BadRequest(Json.parse("{ \"error\": \"unsupported task type: " + t + "\" }"))
            }
          else
            Forbidden(Json.parse("{ \"error\": \"insufficient access privileges\"}"))
        })
        
      case Some(result) if result.isError => Future.successful(BadRequest(Json.parse("{ \"error\": \"JSON parse error\" }")))
      case None => Future.successful(BadRequest(Json.parse("{ \"error\": \"missing task definition\" }")))
    }
  }
    
  def progressByDocument(id: String) = silhouette.SecuredAction.async { implicit request =>    
    documents.getExtendedInfo(id, Some(request.identity.username)).flatMap(_ match {
      case Some((doc, accesslevel)) =>
        if (accesslevel.canReadAll) {
          tasks.findByDocument(id).map {
            case Some(taskRecord) => jsonOk(Json.toJson(taskRecord))
            case None => NotFound
          }
        } else {
          Future.successful(Forbidden)
        }
        
      case None => Future.successful(NotFound)
    })
  }
  
}
