package controllers.admin.gazetteers

import akka.stream.Materializer
import controllers.{ BaseAuthController, WebJarAssets }
import javax.inject.Inject
import models.document.DocumentService
import models.place.{ GazetteerUtils, PlaceService }
import models.user.UserService
import models.user.Roles._
import play.api.{ Configuration, Logger }
import scala.concurrent.{ ExecutionContext, Future }
import java.io.FileInputStream
import models.place.crosswalks.PleiadesCrosswalk
import models.place.crosswalks.GeoNamesCrosswalk

class GazetteerAdminController @Inject() (
    val config: Configuration,
    val documents: DocumentService,
    val places: PlaceService,
    val users: UserService,
    implicit val materializer: Materializer,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarAssets
  ) extends BaseAuthController(config, documents, users) {
  
  def index = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    places.listGazetteers().map { gazetteers => 
      Ok(views.html.admin.gazetteers.index(gazetteers))
    }
  }
  
  def importGazetteer = StackAction(AuthorityKey -> Admin) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("gazetteer-file")) match {
      case Some(formData) => {
        Logger.info("Importing gazetteer from " + formData.filename)
        val importer = new StreamImporter()
        
        
        
        /** TEMPORARY HACK **/
        
        if (formData.filename.toLowerCase.contains("pleiades")) {
          Logger.info("Using Pleiades crosswalk")
          importer.importPlaces(new FileInputStream(formData.ref.file), PleiadesCrosswalk.fromJson)(places, ctx)
        } else if (formData.filename.toLowerCase.contains("geonames")) {
          Logger.info("Using GeoNames crosswalk")
          importer.importPlaces(new FileInputStream(formData.ref.file), GeoNamesCrosswalk.fromJson)(places, ctx)
        }

        /** TEMPORARY HACK **/
        
          
          
          
        Redirect(routes.GazetteerAdminController.index)
      }
        
      case None => BadRequest
        
    }
  }
  
  def deleteGazetteer(name: String) = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    places.deleteByGazetteer(name).map { _ =>
      Status(200)
    }
  }

}