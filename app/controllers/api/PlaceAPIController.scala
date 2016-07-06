package controllers.api

import controllers.{ BaseOptAuthController, HasPrettyPrintJSON }
import javax.inject.Inject
import models.user.Roles._
import models.place.PlaceService
import play.api.cache.CacheApi
import play.api.libs.json.Json
import play.api.mvc.Action
import scala.concurrent.{ ExecutionContext, Future }
import storage.DB

class PlaceAPIController @Inject() (implicit val cache: CacheApi, val db: DB, context: ExecutionContext) extends BaseOptAuthController with HasPrettyPrintJSON {
    
  /** Lookup by URI - open to all, so that it's available to public documents **/
  def findPlaceByURI(uri: String) = Action.async { implicit request =>
    PlaceService.findByURI(uri).map { _ match {
      case Some((place, _)) => jsonOk(Json.toJson(place))
      case None => NotFound
    }}
  }

  /** Search by query string - available to logged in users only **/
  def searchPlaces(query: String, offset: Int, size: Int) = AsyncStack { implicit request =>
    loggedIn match {
      case Some(user) => PlaceService.searchPlaces(query, offset, size).map { results => 
        jsonOk(Json.toJson(results.map(_._1)))
      }
      
      case None => Future.successful(Forbidden)
    }
  }
  
  /** List all places in document - bound to the current user's access level **/
  def listPlacesInDocument(docId: String, offset: Int, size: Int) = AsyncStack { implicit request =>
    documentResponse(docId, loggedIn.map(_.user.getUsername), { case (document, fileparts, accesslevel) =>
      if (accesslevel.canRead)
        PlaceService.listPlacesInDocument(docId, offset, size).map(tuples => jsonOk(Json.toJson(tuples.map(_._1))))
      else
        Future.successful(Forbidden)
    })
  }
  
  /** Search places in document - bound to the current user's access level **/
  def searchPlacesInDocument(query: String, docId: String, offset: Int, size: Int) = AsyncStack { implicit request =>
    documentResponse(docId, loggedIn.map(_.user.getUsername), { case (document, fileparts, accesslevel) =>
      if (accesslevel.canRead)
        PlaceService.searchPlacesInDocument(query, docId, offset, size).map { results =>
          jsonOk(Json.toJson(results.map(_._1)))
        }
      else
        Future.successful(Forbidden)
    })
  }
 
}