package controllers.api

import akka.actor.ActorSystem
import controllers.{ BaseAuthController, HasPrettyPrintJSON }
import java.util.UUID
import javax.inject.Inject
import models.document.{ DocumentInfo, DocumentService }
import models.task.TaskType
import models.user.UserService
import models.user.Roles._
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.{ ExecutionContext, Future }
import transform.georesolution.GeoresolutionService

case class TaskDefinition(
    
  taskType: TaskType,
  
  documentId: String,
  
  filepartId: Option[UUID]
  
)

object TaskDefinition {

  implicit val taskDefinitonReads: Reads[TaskDefinition] = (
    (JsPath \ "task_type").read[String].map(str => TaskType(str)) and
    (JsPath \ "document_id").read[String] and
    (JsPath \ "filepart_id").readNullable[UUID]
  )(TaskDefinition.apply _)

}

class TaskAPIController @Inject() (
    val config: Configuration,
    val documents: DocumentService,
    val users: UserService,
    val georesolution: GeoresolutionService,
    implicit val system: ActorSystem,
    implicit val ctx: ExecutionContext
  ) extends BaseAuthController(config, documents, users) with HasPrettyPrintJSON {

  def spawnTask = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    request.body.asJson.map(json => Json.fromJson[TaskDefinition](json)) match {
      case Some(result) if result.isSuccess =>
        val taskDefinition = result.get
        documentResponse(taskDefinition.documentId, loggedIn.user, { case (docInfo, accesslevel) =>
          if (accesslevel.canWrite)
            taskDefinition.taskType match {  
              case TaskType("GEORESOLUTION") =>
                georesolution.spawnTask(docInfo.document, docInfo.fileparts)
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
  
  def queryProgress(id: UUID) = StackAction(AuthorityKey -> Normal) { implicit request =>
    Ok
  }
  
}