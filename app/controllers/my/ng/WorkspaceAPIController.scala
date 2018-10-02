package controllers.my.ng

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{Action, ControllerComponents}
import scala.concurrent.ExecutionContext
import services.document.DocumentService
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.user.UserService
import storage.uploads.Uploads

/** A quick hack for local testing of the new React UI **/
@Singleton
class WorkspaceAPIController @Inject() (
    val components: ControllerComponents,
    val documents: DocumentService,
    val silhouette: Silhouette[Security.Env],
    val users: UserService,
    val uploads: Uploads,
    val config: Configuration,
    implicit val ctx: ExecutionContext
  ) extends BaseController(components, config, users)
      with HasPrettyPrintJSON {
  
  // DocumentRecord JSON serialization     
  import services.document.DocumentService.documentRecordWrites
    
  implicit val documentWithMetadataWrites: Writes[(DocumentRecord, Seq[DocumentFilepartRecord])] = (
    (JsPath).write[DocumentRecord] and
    (JsPath \ "filetypes").write[Seq[String]]
  )(t => (
    t._1,
    t._2.map(_.getContentType)
  ))
  
  def me = Action.async { implicit request =>
    users.findByUsername("rainer").map { user =>
      val usedMB = Math.round(100 * uploads.getUsedDiskspaceKB("rainer") / 1024).toDouble / 100
      import services.user.User._
      val json = 
        Json.toJson(user.get.record).as[JsObject] ++ Json.obj("used_mb" -> usedMB)
      jsonOk(json).withHeaders("Access-Control-Allow-Origin" -> "*")
    }
  }
  
  // Quick hack for testing  + CORS
  def myDocuments(offset: Int, size: Int) = Action.async { implicit request =>
    documents.findByOwnerWithPartMetadata("rainer", offset, size).map { documents =>
      jsonOk(Json.toJson(documents.toSeq)).withHeaders("Access-Control-Allow-Origin" -> "*")
    }
  }
  
  def sharedWithMe(offset: Int, size: Int) = Action.async { implicit request =>
    documents.findBySharedWith("username", offset, size).map { documents =>
      jsonOk(Json.toJson(documents.items.map(_._1))).withHeaders("Access-Control-Allow-Origin" -> "*")
    }
  }

}