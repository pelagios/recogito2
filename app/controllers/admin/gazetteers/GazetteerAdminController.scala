package controllers.admin.gazetteers

import controllers.WebJarAssets
import controllers.BaseAuthController
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import play.api.Logger
import scala.concurrent.{ ExecutionContext, Future }
import storage.DB
import models.place.{ GazetteerUtils, PlaceService }
import controllers.HasPrettyPrintJSON
import play.api.libs.json.Json

class GazetteerAdminController @Inject() (implicit val cache: CacheApi, val db: DB, ec: ExecutionContext, webjars: WebJarAssets) extends BaseAuthController {
  
  def index = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    PlaceService.listGazetteers().map { gazetteers => 
      Ok(views.html.admin.gazetteers.index(gazetteers))
    }
  }
  
  def importGazetteer = StackAction(AuthorityKey -> Admin) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("gazetteer-file")) match {
      case Some(formData) => {
        Future {
          scala.concurrent.blocking {
            Logger.info("Importing gazetteer from " + formData.filename)
            GazetteerUtils.importRDFStream(formData.ref.file, formData.filename)
          }
        }
        Redirect(routes.GazetteerAdminController.index)
      }
        
      case None => BadRequest
        
    }
  }
  
  def deleteGazetteer(name: String) = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    PlaceService.deleteByGazetteer(name).map { _ =>
      Status(200)
    }
  }

}