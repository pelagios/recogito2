package controllers.api

import controllers.{ BaseAuthController, HasPrettyPrintJSON }
import javax.inject.Inject
import models.user.Roles._
import models.place.PlaceService
import play.api.cache.CacheApi
import play.api.libs.json.Json
import play.api.mvc.Action
import scala.concurrent.ExecutionContext
import storage.DB

class PlaceAPIController @Inject() (implicit val cache: CacheApi, val db: DB, context: ExecutionContext) extends BaseAuthController with HasPrettyPrintJSON {
    
  /** This method is open to all, so it works with public documents **/
  def findPlaceByURI(uri: String) = Action.async { implicit request =>
    PlaceService.findByURI(uri).map { _ match {
      case Some((place, _)) => jsonOk(Json.toJson(place))
      case None => NotFound
    }}
  }

  /** Available to logged in users only **/
  def searchPlaces(query: String, offset: Int, size: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    PlaceService.searchPlaces(query, offset, size).map { results => 
      jsonOk(Json.toJson(results.map(_._1)))
    }
  }
  
  /** TODO bind availability to access level **/
  def listPlacesInDocument(docId: String, offset: Int, size: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    PlaceService.listPlacesInDocument(docId, offset, size).map(tuples => jsonOk(Json.toJson(tuples.map(_._1))))
  }
  
  /** TODO bind availability to access level **/
  def searchPlacesInDocument(query: String, docId: String, offset: Int, size: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    PlaceService.searchPlacesInDocument(query, docId, offset, size).map { results =>
      jsonOk(Json.toJson(results.map(_._1)))
    }
  }
 
}