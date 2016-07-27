package controllers.api

import com.vividsolutions.jts.geom.Coordinate
import controllers.{ BaseOptAuthController, ControllerContext, HasPrettyPrintJSON }
import javax.inject.Inject
import models.user.Roles._
import models.place.PlaceService
import play.api.libs.json.Json
import play.api.mvc.Action
import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

class PlaceAPIController @Inject() (placeService: PlaceService, implicit val ctx: ControllerContext) extends BaseOptAuthController with HasPrettyPrintJSON {
    
  /** Lookup by URI - open to all, so that it's available to public documents **/
  def findPlaceByURI(uri: String) = Action.async { implicit request =>
    placeService.findByURI(uri).map { _ match {
      case Some((place, _)) => jsonOk(Json.toJson(place))
      case None => NotFoundPage
    }}
  }

  /** Search by query string - available to logged in users only **/
  def searchPlaces(query: String, offset: Int, size: Int, latlng: Option[String]) = AsyncStack { implicit request =>
    loggedIn match {
      case Some(user) => {
        val sortFrom = latlng.flatMap { l =>
          val arg = l.split(",")
          Try(new Coordinate(arg(1).toDouble, arg(0).toDouble)) match {
            case Success(coord) => Some(coord)
            case Failure(e) => None
          }
        }

        placeService.searchPlaces(query, offset, size, sortFrom).map { results => 
          jsonOk(Json.toJson(results.map(_._1)))
        }
      }
      
      case None => Future.successful(ForbiddenPage)
    }
  }
  
  /** List all places in document - bound to the current user's access level **/
  def listPlacesInDocument(docId: String, offset: Int, size: Int) = AsyncStack { implicit request =>
    documentResponse(docId, loggedIn.map(_.user.getUsername), { case (document, fileparts, accesslevel) =>
      if (accesslevel.canRead)
        placeService.listPlacesInDocument(docId, offset, size).map(tuples => jsonOk(Json.toJson(tuples.map(_._1))))
      else
        Future.successful(ForbiddenPage)
    })
  }
  
  /** Search places in document - bound to the current user's access level **/
  def searchPlacesInDocument(query: String, docId: String, offset: Int, size: Int) = AsyncStack { implicit request =>
    documentResponse(docId, loggedIn.map(_.user.getUsername), { case (document, fileparts, accesslevel) =>
      if (accesslevel.canRead)
        placeService.searchPlacesInDocument(query, docId, offset, size).map { results =>
          jsonOk(Json.toJson(results.map(_._1)))
        }
      else
        Future.successful(ForbiddenPage)
    })
  }
 
}