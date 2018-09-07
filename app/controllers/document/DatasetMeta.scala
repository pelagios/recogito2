package controllers.document

import com.vividsolutions.jts.geom.Envelope
import play.api.libs.json._
import play.api.mvc.RequestHeader
import services.document.DocumentInfo
import services.HasGeometry

case class DatasetMeta(doc: DocumentInfo, spatialCoverage: Option[Envelope] = None) extends HasGeometry {

  lazy val name = Option(doc.owner.getRealName).getOrElse(doc.ownerName) 
  
  def sameAs()(implicit request: RequestHeader) =
    if (spatialCoverage.isDefined) // Canonical URL 
      JsNull
    else
      JsString(controllers.document.downloads.routes.DownloadsController.showDownloadOptions(doc.id).absoluteURL)

  def asJsonLd()(implicit request: RequestHeader) = Json.prettyPrint(
    JsObject(
      Json.obj(
        "@context" -> "http://schema.org",
        "@type" -> "Dataset",
        "name" -> doc.title,
        "description" -> doc.description, // TODO this is a mandatory parameter in Google's Dataset spec!
        "url" -> controllers.document.annotation.routes.AnnotationController.showAnnotationView(doc.id, 1).absoluteURL,
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
        "spatialCoverage" -> Json.toJson(spatialCoverage)
      ).fields.filter(_._2 match { // Filter out undefined props
        case JsNull => false
        case _ => true
      })
    )
  )
  
}