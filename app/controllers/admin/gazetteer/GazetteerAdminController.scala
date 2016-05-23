package controllers.admin.gazetteer

import controllers.BaseController
import java.io.FileInputStream
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import storage.DB
import models.place.{ GazetteerUtils, PlaceService }

class GazetteerAdminController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController {
  
  def index = StackAction(AuthorityKey -> Admin) { implicit request =>
    Ok(views.html.admin.gazetteers())
  }
  
  def importGazetteer = StackAction(AuthorityKey -> Admin) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("gazetteer-file")) match {
      case Some(formData) => {
        Future {
          Logger.info("Importing gazetteer from " + formData.filename)
          val places = GazetteerUtils.loadRDF(formData.ref.file, formData.filename)
          PlaceService.importRecords(places)
        }
        Ok(views.html.admin.gazetteers())
      }
        
      case None => BadRequest
        
    }
  }
  
}