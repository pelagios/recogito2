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
import models.place.{ Gazetteer, PlaceService }

class GazetteerAdminController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController {
  
  def index = StackAction(AuthorityKey -> Admin) { implicit request =>
    Ok(views.html.admin.gazetteers())
  }
  
  def importGazetteer = StackAction(AuthorityKey -> Admin) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("gazetteer-file")) match {
      case Some(formData) => {
        Future {
          val places = Gazetteer.loadFromRDF(new FileInputStream(formData.ref.file), formData.filename, formData.filename.substring(0, formData.filename.lastIndexOf('.')))
          Logger.info("Importing gazetteer " + formData.filename.substring(0, formData.filename.lastIndexOf('.')))
          PlaceService.importRecords(places)
        }
        Ok(views.html.admin.gazetteers())
      }
        
      case None => BadRequest
        
    }
  }
  
}