package controllers.api.entity

import com.mohiva.play.silhouette.api.Silhouette
import com.vividsolutions.jts.geom.Coordinate
import controllers.{BaseOptAuthController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import services.document.DocumentService
import services.entity.builtin.EntityService
import services.user.UserService
import services.user.Roles._
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import scala.concurrent.{Future, ExecutionContext }
import scala.util.{Try, Success, Failure}
import services.entity.EntityType

@Singleton
class PlaceAPIController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val users: UserService,
  val entities: EntityService,
  val silhouette: Silhouette[Security.Env],
  implicit val ctx: ExecutionContext
) extends BaseOptAuthController(components, config, documents, users) with HasPrettyPrintJSON {

  /** Lookup by URI - open to all, so that it's available to public documents **/
  def findPlaceByURI(uri: String) = Action.async { implicit request =>
    entities.findByURI(uri).map { _ match {
      case Some(e) => jsonOk(Json.toJson(e.entity))
      case None => NotFoundPage
    }}
  }

  /** Search by query string - available to logged in users only **/
  def searchPlaces(
    query: String, 
    offset: Int, 
    size: Int, 
    latlng: Option[String],
    gazetteers:Option[String]
  ) = silhouette.SecuredAction.async { implicit request =>
    // Parse (optional) center coordinate from query string
    val sortFrom = latlng.flatMap { l =>
      val arg = l.split(",")
      Try(new Coordinate(arg(1).toDouble, arg(0).toDouble)) match {
        case Success(coord) => Some(coord)
        case Failure(e) => None
      }
    }
    
    // Parse (optional) gazetteer filter from query string
    val allowedGazetteers = gazetteers.map(_.split(",").map(_.trim).toSeq)

    entities.searchEntities(query, Some(EntityType.PLACE), offset, size, sortFrom, allowedGazetteers).map { results =>
      jsonOk(Json.toJson(results.map(_.entity)))
    }
  }

  /** List all places in document - bound to the current user's access level **/
  def listPlacesInDocument(docId: String, offset: Int, size: Int) = silhouette.UserAwareAction.async { implicit request =>
    documentResponse(docId, request.identity, { case (metadata, accesslevel) =>
      if (accesslevel.canReadData)
        entities.listEntitiesInDocument(docId, Some(EntityType.PLACE), offset, size).map { result =>
          jsonOk(Json.toJson(result.map(_._1.entity)))
        }
      else
        Future.successful(Forbidden)
    })
  }

  /** Search places in document - bound to the current user's access level **/
  def searchPlacesInDocument(query: String, docId: String, offset: Int, size: Int) = silhouette.UserAwareAction.async { implicit request =>
    documentResponse(docId, request.identity, { case (metadata, accesslevel) =>
      if (accesslevel.canReadData)
        entities.searchEntitiesInDocument(query, docId, Some(EntityType.PLACE), offset, size).map { results =>
          jsonOk(Json.toJson(results.map(_.entity)))
        }
      else
        Future.successful(Forbidden)
    })
  }

}
