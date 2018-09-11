package controllers.document

import com.vividsolutions.jts.geom.Envelope
import play.api.libs.json._
import play.api.mvc.RequestHeader
import services.document.DocumentInfo
import services.generated.tables.DocumentFilepart
import services.ContentType

case class DatasetMeta(doc: DocumentInfo, spatialCoverage: Option[Envelope] = None) {

  lazy val name = Option(doc.owner.getRealName).getOrElse(doc.ownerName) 
  
  def image()(implicit request: RequestHeader) = doc.fileparts
      .filter(_.getContentType.startsWith("IMAGE_"))
      .headOption.map { filepart =>
        ContentType.withName(filepart.getContentType).get match {  
          case ContentType.IMAGE_UPLOAD =>
            controllers.document.routes.DocumentController.getImageTile(doc.id, filepart.getSequenceNo, "TileGroup0/2-1-1.jpg").absoluteURL
            
          case ContentType.IMAGE_IIIF =>
            s"${filepart.getFile.substring(0, filepart.getFile.length - 9)}square/256,256/0/default.jpg"
            
          case _ => "" // Can never happen
        }
      }
      
  def sameAs()(implicit request: RequestHeader) =
    if (spatialCoverage.isDefined) // Canonical URL 
      JsNull
    else
      JsString(controllers.document.downloads.routes.DownloadsController.showDownloadOptions(doc.id).absoluteURL)
      
  def toBox(e: Envelope) =
    s"${e.getMinY} ${e.getMinX} ${e.getMaxY} ${e.getMaxX}"
      
  def asJsonLd()(implicit request: RequestHeader) = Json.prettyPrint(
    JsObject(
      Json.obj(
        "@context" -> "http://schema.org",
        "@type" -> "Dataset",
        "name" -> doc.title,
        "description" -> doc.description, // TODO this is a mandatory parameter in Google's Dataset spec!
        "url" -> controllers.document.annotation.routes.AnnotationController.showAnnotationView(doc.id, 1).absoluteURL,
        "image" -> image,
        "sameAs" -> sameAs,
        "creator" -> Json.obj(
          "@type" -> "Person",
          "name" -> name,
          "url" -> controllers.my.routes.MyRecogitoController.index(doc.ownerName, None, None, None, None, None).absoluteURL
        ),
        "distribution" -> Json.arr(
          Json.obj(
            "@type" -> "DataDownload",
            "encodingFormat" -> "CSV",
            "contentUrl" -> controllers.document.downloads.routes.DownloadsController.downloadCSV(doc.id, false).absoluteURL
          ),
          Json.obj(
            "@type" -> "DataDownload",
            "encodingFormat" -> "application/vnd.geo+json",
            "contentUrl" -> controllers.document.downloads.routes.DownloadsController.downloadGeoJSON(doc.id, false).absoluteURL
          )
        ),
        "temporalCoverage" -> doc.dateFreeform, // TODO create structured dates if possible
        "spatialCoverage" -> spatialCoverage.map { env => Json.obj(
          "@type" -> "Place",
          "geo" -> Json.obj(
            "@type" -> "GeoShape",
            "box" -> toBox(env)
          )
        )}
      ).fields.filter(_._2 match { // Filter out undefined props
        case JsNull => false
        case _ => true
      })
    )
  )
  
}