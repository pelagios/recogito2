package controllers.document.downloads

import akka.util.ByteString
import akka.stream.scaladsl.Source
import controllers.BaseOptAuthController
import controllers.document.downloads.serializers._
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.stackc.RequestWithAttributes
import models.annotation.AnnotationService
import models.document.{ DocumentInfo, DocumentService }
import models.place.PlaceService
import models.user.UserService
import org.apache.jena.riot.RDFFormat
import org.webjars.play.WebJarsUtil
import play.api.{ Configuration, Logger }
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.Enumerator
import play.api.libs.Files.TemporaryFileCreator
import play.api.mvc.{ AnyContent, Result }
import play.api.http.{ HttpEntity, FileMimeTypes }
import scala.concurrent.{ ExecutionContext, Future }
import storage.Uploads
import models.ContentType

case class FieldMapping(
  
  // TODO normalize URL
    
  // TODO how to deal with geometry? Support WKT + lat/lon in separate columns?
  
  BASE_URL          : Option[String],
  FIELD_ID          : Int,
  FIELD_TITLE       : Int,
  FIELDS_NAME       : Option[Int],
  FIELD_DESCRIPTION : Option[Int],
  FIELD_COUNTRY     : Option[Int],
  FIELD_LATITUDE    : Option[Int],
  FIELD_LONGITUDE   : Option[Int])
  
object FieldMapping {
  
  implicit val fieldMappingReads: Reads[FieldMapping] = (
    (JsPath \ "base_url").readNullable[String] and
    (JsPath \ "id").read[Int] and
    (JsPath \ "title").read[Int] and
    (JsPath \ "name").readNullable[Int] and
    (JsPath \ "description").readNullable[Int] and
    (JsPath \ "country").readNullable[Int] and
    (JsPath \ "latitude").readNullable[Int] and
    (JsPath \ "longitude").readNullable[Int]
  )(FieldMapping.apply _)
  
}

@Singleton
class DownloadsController @Inject() (
    val config: Configuration,
    val users: UserService,
    implicit val mimeTypes: FileMimeTypes,
    implicit val tmpFile: TemporaryFileCreator,
    implicit val uploads: Uploads,
    implicit val annotations: AnnotationService,
    implicit val documents: DocumentService,
    implicit val places: PlaceService,
    implicit val webjars: WebJarsUtil,
    implicit val ctx: ExecutionContext
  ) extends BaseOptAuthController(config, documents, users)
      with CSVSerializer
      with GeoJSONSerializer
      with RDFSerializer
      with webannotation.WebAnnotationSerializer
      with tei.PlaintextSerializer
      with tei.TEISerializer {
  
  private def download(documentId: String, export: DocumentInfo => Future[Result])(implicit request: RequestWithAttributes[AnyContent]) = {
    documentReadResponse(documentId, loggedIn, { case (docInfo, _) => // Used just for the access permission check
      export(docInfo)
    })
  }

  def showDownloadOptions(documentId: String) = AsyncStack { implicit request =>
    documentReadResponse(documentId, loggedIn, { case (doc, accesslevel) =>
      annotations.countByDocId(documentId).map { documentAnnotationCount =>
        Ok(views.html.document.downloads.index(doc, loggedIn, accesslevel, documentAnnotationCount))
      }
    })
  }

  /** Exports either 'plain' annotation CSV, or merges annotations with original DATA_* uploads, if any **/
  def downloadCSV(documentId: String, exportTables: Boolean) = AsyncStack { implicit request =>
    download(documentId, { doc =>
      if (exportTables)
        exportMergedTables(doc).map { case (file, filename) =>
          Ok.sendFile(file).withHeaders(CONTENT_DISPOSITION -> { "attachment; filename=" + filename })
        }
      else
        annotationsToCSV(documentId).map { csv =>
          Ok.sendFile(csv).withHeaders(CONTENT_DISPOSITION -> { "attachment; filename=" + documentId + ".csv" })
        }
    })
  }
  
  private def downloadRDF(documentId: String, format: RDFFormat, extension: String) = AsyncStack { implicit request =>
    download(documentId, { doc =>
      documentToRDF(doc, format).map { file => 
        Ok.sendFile(file).withHeaders(CONTENT_DISPOSITION -> { "attachment; filename=" + documentId + "." + extension })
      }
    })
  }
  
  def downloadTTL(documentId: String) = downloadRDF(documentId, RDFFormat.TTL, "ttl") 
  def downloadRDFXML(documentId: String) = downloadRDF(documentId, RDFFormat.RDFXML, "rdf.xml") 
  
  def downloadJSONLD(documentId: String) = AsyncStack { implicit request =>
    download(documentId, { doc =>
      documentToWebAnnotation(doc).map { json =>
        Ok(Json.prettyPrint(json))
          .withHeaders(CONTENT_DISPOSITION -> { "attachment; filename=" + documentId + ".jsonld" })
      }
    })
  }

  def downloadGeoJSON(documentId: String, asGazetteer: Boolean) = AsyncStack { implicit request =>
    
    // Standard GeoJSON download
    def downloadPlaces() =
      placesToGeoJSON(documentId).map { featureCollection =>
        Ok(Json.prettyPrint(featureCollection))
          .withHeaders(CONTENT_DISPOSITION -> { "attachment; filename=" + documentId + ".json" })
      }
    
    // Places + spreadsheet info, according to Pelagios gazetteer GeoJSON conventions
    def downloadGazetteer(doc: DocumentInfo) = request.body.asFormUrlEncoded.flatMap(_.get("json").flatMap(_.headOption)) match {
      case Some(str) =>
        Json.fromJson[FieldMapping](Json.parse(str)) match {
          case result: JsSuccess[FieldMapping] =>
            exportGeoJSONGazetteer(doc, result.get).map { featureCollection =>
              Ok(Json.prettyPrint(featureCollection))
                .withHeaders(CONTENT_DISPOSITION -> { "attachment; filename=" + documentId + ".json" })
            }
              
          case error: JsError =>
            Logger.warn("Attempt to download gazetteer but field mapping invalid: " + str + "\n" + error)
            Future.successful(BadRequest)
        }
          
      case None =>
        Logger.warn("Attempt to download gazetteer without field mapping payload")
        Future.successful(BadRequest)
    }

    
    download(documentId, { doc =>
      if (asGazetteer)
        downloadGazetteer(doc)
      else
        downloadPlaces
    })
  }
  
  def downloadTEI(documentId: String) = AsyncStack { implicit request =>
    download(documentId, { doc =>
      val contentTypes = doc.fileparts.flatMap(pt => ContentType.withName(pt.getContentType)).distinct
      
      // At the moment, we only support TEI download for documents that are either plaintext- or TEI-only
      if (contentTypes.size != 1) {
        Future.successful(BadRequest("unsupported document content type(s)"))
      } else if (!contentTypes.head.isText) {
        Future.successful(BadRequest("unsupported document content type(s)"))
      } else {
        val f = contentTypes.head match {
          case ContentType.TEXT_PLAIN => plaintextToTEI(doc)
          case ContentType.TEXT_TEIXML => teiToTEI(doc)
          case _ => throw new Exception // Can't happen under current conditions (and should fail in the future if things go wrong)
        }
        
        f.map(xml => Ok(xml).withHeaders(CONTENT_DISPOSITION -> { "attachment; filename=" + documentId + ".tei.xml" }))
      }
    })
  }

}
