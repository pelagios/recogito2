package controllers.admin.gazetteers

import akka.stream.Materializer
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import java.io.FileInputStream
import javax.inject.{Inject, Singleton}
import services.document.DocumentService
import services.entity.{EntityService, EntityType}
import services.entity.crosswalks.geojson._
import services.entity.crosswalks.rdf._
import services.entity.importer.EntityImporter
import services.user.UserService
import services.user.Roles._
import org.webjars.play.WebJarsUtil
import play.api.{Configuration, Logger}
import play.api.mvc.ControllerComponents
import scala.concurrent.{ExecutionContext, Future}
import storage.ES

@Singleton
class GazetteerAdminController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val entities: EntityService,
  val users: UserService,
  val es: ES,
  val silhouette: Silhouette[Security.Env],
  implicit val materializer: Materializer,
  implicit val ctx: ExecutionContext,
  implicit val webJarsUtil: WebJarsUtil
) extends BaseAuthController(components, config, documents, users) {
  
  def index = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    entities.listEntities(Some(EntityType.PLACE)).map { gazetteers => 
      Ok(views.html.admin.gazetteers.index(gazetteers.items.map(_.entity.title)))
    }
  }
  
  def importGazetteer = silhouette.SecuredAction(Security.WithRole(Admin)) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("gazetteer-file")) match {
      case Some(formData) => {
        
        Logger.info("Importing gazetteer from " + formData.filename)
          
        /** TEMPORARY HACK **/
        val file = formData.ref.path.toFile
        val importer = new EntityImporter(entities, EntityType.PLACE, es, ctx)
        
        if (formData.filename.contains(".ttl") || formData.filename.contains(".rdf") || formData.filename.contains(".xml")) {
          Logger.info("Importing Pelagios RDF dump")
          val loader = new DumpLoader()          
          loader.importDump(file, formData.filename, PelagiosRDFCrosswalk.fromRDF(formData.filename), importer)
        } else if (formData.filename.toLowerCase.contains("pleiades")) {
          Logger.info("Using Pleiades crosswalk")
          val loader = new StreamLoader()
          loader.importPlaces(new FileInputStream(file), PleiadesCrosswalk.fromJson, importer)
        } else if (formData.filename.toLowerCase.contains("geonames")) {
          Logger.info("Using GeoNames crosswalk")
          val loader = new StreamLoader()
          loader.importPlaces(new FileInputStream(file), GeoNamesCrosswalk.fromJson, importer)
        } else if (formData.filename.endsWith("json")) {
          Logger.info("Importing Pelagios GeoJSON FeatureCollection")
          val loader = new DumpLoader()
          loader.importDump(file, formData.filename, PelagiosGeoJSONCrosswalk.fromGeoJSON(formData.filename), importer)
        }

        /** TEMPORARY HACK **/
        
          
          
          
        Redirect(routes.GazetteerAdminController.index)
      }
        
      case None => BadRequest
        
    }
  }
  
  def deleteGazetteer(name: String) = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    entities.deleteByAuthoritySource(name).map { _ =>
      Status(200)
    }
  }

}