package controllers.admin.gazetteers

import akka.stream.Materializer
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import java.io.{File, FileInputStream}
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json, JsObject}
import play.api.mvc.ControllerComponents
import services.document.DocumentService
import services.entity.{AuthorityFileService, EntityType}
import services.entity.builtin.EntityService
import services.entity.builtin.importer.crosswalks.geojson._
import services.entity.builtin.importer.crosswalks.rdf._
import services.entity.builtin.importer.EntityImporterFactory
import services.generated.tables.records.AuthorityFileRecord
import services.user.UserService
import services.user.Roles._
import org.webjars.play.WebJarsUtil
import scala.concurrent.{ExecutionContext, Future}
import storage.es.ES
import controllers.HasPrettyPrintJSON

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
) extends BaseAuthController(components, config, documents, users) with HasPrettyPrintJSON {
  

 
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
  
  def listGazetteers = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    val fMetadata = authorities.listAll(Some(EntityType.PLACE))
    val fRecordCounts = entities.countByAuthority(Some(EntityType.PLACE))
    
    val f = for {
      metadata <- fMetadata
      counts <- fRecordCounts
    } yield (metadata, counts)
    
    f.map { case (listedGazetteers, counts) =>
      import AuthorityFileService._ // Import implicit JSON serialization
      
      // In order to see possible inconsistencies between DB and index,
      // we separately handle: i) gazetteers described in the DB, ii) gazetteer
      // IDs recorded in the index, but without a metadata record in the DB.
      // Normally, the list below should be empty.
      val unlistedGazetteers = counts.filterNot { case (id, count) => 
        listedGazetteers.map(_.getId).contains(id) 
      }
      
      val json = listedGazetteers.map {  m =>
          val count = counts.find(_._1 == m.getId).map(_._2).getOrElse(0l)
          Json.toJson(m).as[JsObject] ++ Json.obj("count" -> count)
        } ++ unlistedGazetteers.map { case (id, count) =>
          Json.obj(
            "identifier" -> id,
            "authority_type" -> "PLACE",
            "shortname" -> id,
            "count" -> count,
            "conflicted" -> true)
        }
      
      jsonOk(Json.toJson(json))
    }
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
          EntityType.PLACE, // TODO hard-wired temporarily only
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