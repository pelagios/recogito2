package controllers.admin.gazetteers

import akka.stream.Materializer
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import java.io.FileInputStream
import javax.inject.{Inject, Singleton}
import services.document.DocumentService
import services.entity.EntityType
import services.entity.builtin.EntityService
import services.entity.builtin.importer.crosswalks.geojson._
import services.entity.builtin.importer.crosswalks.rdf._
import services.entity.builtin.importer.EntityImporterFactory
import services.user.UserService
import services.user.Roles._
import org.webjars.play.WebJarsUtil
import play.api.{Configuration, Logger}
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import storage.es.ES

@Singleton
class GazetteerAdminController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val entities: EntityService,
  val importerFactory: EntityImporterFactory,
  val users: UserService,
  val silhouette: Silhouette[Security.Env],
  implicit val materializer: Materializer,
  implicit val ctx: ExecutionContext,
  implicit val webJarsUtil: WebJarsUtil
) extends BaseAuthController(components, config, documents, users) {
  
  def index = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    entities.listAuthorities(Some(EntityType.PLACE)).map { gazetteers => 
      Ok(views.html.admin.gazetteers.index(gazetteers.map(_._1)))
    }
  }
  
  def importGazetteer = silhouette.SecuredAction(Security.WithRole(Admin)) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("file")) match {
      case Some(formData) => {
        
        Logger.info("Importing gazetteer from " + formData.filename)
          
        /** TEMPORARY HACK **/
        
        val file = formData.ref.path.toFile
        val importer = importerFactory.createImporter(EntityType.PLACE)
        
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
    entities.deleteBySourceAuthority(name).map { success =>
      if (success) Logger.info("Delete complete. Everything fine. Congratulations")
      else Logger.warn("Delete complete but something went wrong.")
      Status(200)
    }
  }

}