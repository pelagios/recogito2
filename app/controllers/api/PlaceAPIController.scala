package controllers.api

import controllers.BaseController
import javax.inject.Inject
import models.user.Roles._
import models.place.PlaceService
import play.api.cache.CacheApi
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import storage.DB

class PlaceAPIController @Inject() (implicit val cache: CacheApi, val db: DB, val ec: ExecutionContext) extends BaseController {
  
  /** Search on the entire gazetteer (use case: geo-resolution) is restricted to logged-in users **/ 
  def search(query: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    PlaceService.searchPlaces(query).map { results => 
      Ok(Json.prettyPrint(Json.toJson(results.map(_._1))))
    }
  }
  
}