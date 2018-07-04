package controllers.api

import controllers.{BaseOptAuthController, HasPrettyPrintJSON}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{Json, JsObject}
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import services.document.DocumentService
import services.entity.{AuthorityFileService, EntityType}
import services.entity.builtin.EntityService
import services.user.UserService

@Singleton
class AuthoritiesAPIController @Inject() (
  val authorities: AuthorityFileService,
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val entities: EntityService,
  val users: UserService,
  implicit val ctx: ExecutionContext
) extends BaseOptAuthController(components, config, documents, users) with HasPrettyPrintJSON {

  def listGazetteers = Action.async { implicit request =>
    val fMetadata = authorities.listAll(Some(EntityType.PLACE))
    val fRecordCounts = entities.countByAuthority(Some(EntityType.PLACE))
    
    val f = for {
      metadata <- fMetadata
      counts <- fRecordCounts
    } yield (metadata, counts)
    
    f.map { case (listedGazetteers, counts) =>
      import AuthorityFileService._ // Import implicit JSON serialization
      
      // In order to see possible inconsistencies between DB and index,
      // we separately handle: i) gazetteers described in the DB, ii) gazetteer
      // IDs recorded in the index, but without a metadata record in the DB.
      // Normally, the list below should be empty.
      val unlistedGazetteers = counts.filterNot { case (id, count) => 
        listedGazetteers.map(_.getId).contains(id) 
      }
      
      val json = listedGazetteers.map {  m =>
          val count = counts.find(_._1 == m.getId).map(_._2).getOrElse(0l)
          Json.toJson(m).as[JsObject] ++ Json.obj("count" -> count)
        } ++ unlistedGazetteers.map { case (id, count) =>
          Json.obj(
            "identifier" -> id,
            "authority_type" -> "PLACE",
            "shortname" -> id,
            "count" -> count,
            "conflicted" -> true)
        }
      
      jsonOk(Json.toJson(json))
    }
  }

}