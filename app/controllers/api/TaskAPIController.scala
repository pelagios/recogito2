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
import transform.ner.NERService

case class TaskDefinition (
  taskType: TaskType, documents : Seq[String], fileparts : Seq[UUID]
)

object TaskDefinition {

  implicit val taskDefinitionReads: Reads[TaskDefinition] = (
    (JsPath \ "task_type").read[String].map(str => TaskType(str)) and
    (JsPath \ "documents").read[Seq[String]] and
    (JsPath \ "fileparts").readNullable[Seq[UUID]]
      .map(_.getOrElse(Seq.empty[UUID]))
  )(TaskDefinition.apply _)

}

trait SpecificTaskDefinition {

  protected val baseDef: TaskDefinition 

  val task_type = baseDef.taskType

  val documents = baseDef.documents

  val fileparts = baseDef.fileparts

}

case class GeoresolutionTaskDefinition(
  protected val baseDef: TaskDefinition, args: Map[String, String]
) extends SpecificTaskDefinition

object GeoresolutionTaskDefinition {

  implicit val georesolutionTaskDefinitionReads: Reads[GeoresolutionTaskDefinition] = (
    (JsPath).read[TaskDefinition] and
    (JsPath \ "args").read[Map[String, String]]
  )(GeoresolutionTaskDefinition.apply _)

}

case class NERTaskDefinition(
  protected val baseDef: TaskDefinition,
  engine: String, 
  useAllAuthorities: Boolean,
  authorities: Seq[String]
) extends SpecificTaskDefinition

object NERTaskDefinition {

  implicit val georesolutionTaskDefinitionReads: Reads[NERTaskDefinition] = (
    (JsPath).read[TaskDefinition] and
    (JsPath \ "engine").read[String] and
    (JsPath \ "all_authorities").readNullable[Boolean]
      .map(_.getOrElse(false)) and 
    (JsPath \ "authorities").readNullable[Seq[String]]
      .map(_.getOrElse(Seq.empty[String]))
  )(NERTaskDefinition.apply _)

}

@Singleton
class TaskAPIController @Inject() (
    val components: ControllerComponents,
    val config: Configuration,
    val documents: DocumentService,
    val users: UserService,
    val ner: NERService,
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
        documentResponse(taskDefinition.documents.head, request.identity, { case (docInfo, accesslevel) =>
          if (accesslevel.canWrite)
            taskDefinition.taskType match {  
              case TaskType("GEORESOLUTION") =>
                val definition = Json.fromJson[GeoresolutionTaskDefinition](request.body.asJson.get).get
                georesolution.spawnTask(docInfo.document, docInfo.fileparts, definition.args)
                Ok

              case TaskType("NER") =>
                val definition = Json.fromJson[NERTaskDefinition](request.body.asJson.get).get

                // TODO redesign API to make use of task definiton
                ner.spawnTask(docInfo.document, docInfo.fileparts)
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
