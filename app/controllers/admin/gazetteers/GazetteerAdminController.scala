package controllers.admin.gazetteers

import akka.stream.Materializer
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import java.io.{File, FileInputStream}
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.ControllerComponents
import services.document.DocumentService
import services.entity.{AuthorityFileService, EntityType}
import services.entity.builtin.EntityService
import services.entity.builtin.importer.crosswalks.geojson._
import services.entity.builtin.importer.crosswalks.rdf._
import services.entity.builtin.importer.EntityImporterFactory
import services.user.UserService
import services.user.Roles._
import org.webjars.play.WebJarsUtil
import scala.concurrent.{ExecutionContext, Future}
import storage.es.ES

case class AuthorityMetadata(
  identifier  : String,
  shortname   : String,
  fullname    : Option[String],
  shortcode   : Option[String],
  urlPatterns : Option[String],
  color       : Option[String])

@Singleton
class GazetteerAdminController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val authorities: AuthorityFileService,
  val entities: EntityService,
  val importerFactory: EntityImporterFactory,
  val users: UserService,
  val silhouette: Silhouette[Security.Env],
  implicit val materializer: Materializer,
  implicit val ctx: ExecutionContext,
  implicit val webJarsUtil: WebJarsUtil
) extends BaseAuthController(components, config, documents, users) {
 
  val authorityMetadataForm = Form(
    mapping(
      "identifier" -> nonEmptyText,
      "shortname" -> nonEmptyText,
      "fullname" -> optional(text),
      "shortcode" -> optional(text),
      "urlpatterns" -> optional(text),
      "color" -> optional(text)
    )(AuthorityMetadata.apply)(AuthorityMetadata.unapply)
  )
  
  def index = silhouette.SecuredAction(Security.WithRole(Admin)) { implicit request =>
    Ok(views.html.admin.gazetteers.index())
  }
  
  def upsertAuthority = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    authorityMetadataForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest), // Can never happen from the UI
        
      authorityMeta => {
        
        play.api.Logger.info("Updating...")
        
        val urlPatterns = authorityMeta.urlPatterns.map { urls =>
          urls.split(",").map(_.trim).toSeq
        }.getOrElse(Seq.empty[String])

        val maybeImport = request.body.asMultipartFormData
          .flatMap(_.file("file"))
          .map { formData => 
            importDumpfile(formData.ref.path.toFile, formData.filename, authorityMeta.identifier)
          }
          
        authorities.upsert(
          authorityMeta.identifier,
          authorityMeta.shortname,
          authorityMeta.fullname,
          authorityMeta.shortcode,
          authorityMeta.color,
          urlPatterns).map { _ => maybeImport match {
              case Some(_) => Ok("Started file import")
              case None => Ok("Stored successfully")            
            }
          }.recover { case t: Throwable =>
            InternalServerError(t.getMessage)
          }
      }
    )
  }
  
  /** Temporary hack... **/
  private def importDumpfile(file: File, filename: String, identifier: String) = {
    val importer = importerFactory.createImporter(EntityType.PLACE)
    filename.toLowerCase match {
      case f if f.contains(".ttl") | f.contains(".rdf") | f.contains(".xml") =>
        Logger.info("Importing Pelagios RDF dump")
        val loader = new DumpLoader()          
        loader.importDump(file, filename, PelagiosRDFCrosswalk.fromRDF(filename, identifier), importer) 

      case f if f.contains("pleiades") =>
        Logger.info("Using Pleiades crosswalk")
        val loader = new StreamLoader()
        loader.importPlaces(new FileInputStream(file), PleiadesCrosswalk.fromJson, importer)
        
      case f if f.contains("geonames") =>
        Logger.info("Using GeoNames crosswalk")
        val loader = new StreamLoader()
        loader.importPlaces(new FileInputStream(file), GeoNamesCrosswalk.fromJson, importer)
        
      case f if f.endsWith("json") =>
        Logger.info("Importing Pelagios GeoJSON FeatureCollection")
        val loader = new DumpLoader()
        loader.importDump(file, filename, PelagiosGeoJSONCrosswalk.fromGeoJSON(filename), importer)        
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