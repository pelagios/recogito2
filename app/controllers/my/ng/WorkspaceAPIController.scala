package controllers.my.ng

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{Action, ControllerComponents}
import scala.concurrent.ExecutionContext
import services.HasDate
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
      with HasPrettyPrintJSON 
      with HasDate {
  
  // DocumentRecord JSON serialization     
  import services.document.DocumentService.documentRecordWrites
    
  implicit val documentWithMetadataWrites: Writes[(DocumentRecord, Seq[DocumentFilepartRecord])] = (
    (JsPath).write[DocumentRecord] and
    (JsPath \ "filetypes").write[Seq[String]]
  )(t => (
    t._1,
    t._2.map(_.getContentType)
  ))

  /** Returns account information **/
  def account = silhouette.SecuredAction.async { implicit request =>
    val username = request.identity.username

    val fUser = users.findByUsername(username)
    val fMyDocCount = documents.countByOwner(username)
    val fSharedCount = documents.countBySharedWith(username)

    val f = for {
      user <- fUser
      myDocCount <- fMyDocCount
      sharedCount <- fSharedCount
    } yield (user.get, myDocCount, sharedCount)

    f.map { case (user, myDocCount, sharedCount) =>
      val usedMb = Math.round(100 * uploads.getUsedDiskspaceKB(username) / 1024).toDouble / 100

      val json = 
        Json.obj(
          "username" -> user.username,
          "real_name" -> user.realName,
          "member_since" -> formatDate(new DateTime(user.memberSince.getTime)),
          "documents" -> Json.obj(
            "my_documents" -> myDocCount,
            "shared_with_me" -> sharedCount
          ),
          "storage" -> Json.obj(
            "quota_mb" -> user.quotaMb.toInt,
            "used_mb" -> usedMb
          )
        )

      // TODO hack for testing only!
      jsonOk(json).withHeaders(
        "Access-Control-Allow-Origin" -> "http://localhost:7171",
        "Access-Control-Allow-Credentials" -> "true"
      )
    }
  }
  
  /** Returns the list of my documents, taking into account user-specified col/sort config **/
  def myDocuments(offset: Int, size: Int) = silhouette.SecuredAction.async { implicit request =>

    // val config = request.body.asJson

    documents.findByOwnerWithPartMetadata(request.identity.username, offset, size).map { documents =>
      // TODO hack for testing only!
      jsonOk(Json.toJson(documents.toSeq)).withHeaders(
        "Access-Control-Allow-Origin" -> "http://localhost:7171",
        "Access-Control-Allow-Credentials" -> "true"
      )
    }
  }
  
  /** Returns the list of documents shared with me, taking into account user-specified col/sort config **/
  def sharedWithMe(offset: Int, size: Int) = Action.async { implicit request =>
    documents.findBySharedWith("username", offset, size).map { documents =>

      // TODO hack for testing only!
      jsonOk(Json.toJson(documents.items.map(_._1))).withHeaders(
        "Access-Control-Allow-Origin" -> "http://localhost:7171",
        "Access-Control-Allow-Credentials" -> "true"
      )
    }
  }

  def options(path: String) = Action { implicit request => 
    Ok.withHeaders(
      "Access-Control-Allow-Origin" -> "http://localhost:7171",
      "Access-Control-Allow-Credentials" -> "true",
      "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS, PUT, DELETE",
      "Access-Control-Allow-Headers" -> "Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With"
    )
  }

}