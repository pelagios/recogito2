package controllers.document.downloads.serializers

import com.vividsolutions.jts.geom.Geometry
import models.HasGeometry
import models.place.PlaceService
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext

trait GeoJSONSerializer extends BaseSerializer {

  def placesToGeoJSON(documentId: String)(implicit placeService: PlaceService, ctx: ExecutionContext) =
    placeService.listPlacesInDocument(documentId).map { places =>
      val features = places.items.flatMap { case (place, _) =>
        place.geometry.map(geometry => GeoJSONFeature(geometry, place.labels.mkString(", ")))
      }

      GeoJSONFeatureCollection(features)
    }

}

case class GeoJSONFeature(geometry: Geometry, labels: String) {

  lazy val propertiesAsJSON = JsObject(
    Seq("name" -> JsString(labels))
  )

}

object GeoJSONFeature extends HasGeometry {
  
  implicit val geoJSONFeatureWrites: Writes[GeoJSONFeature] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "geometry").write[Geometry] and
    (JsPath \ "properties").write[JsObject]
    // TODO properties
  )(f => ("Feature", f.geometry, f.propertiesAsJSON))
  
}

case class GeoJSONFeatureCollection(features: Seq[GeoJSONFeature])

object GeoJSONFeatureCollection {
  
  implicit val geoJSONFeatureCollectionWrites: Writes[GeoJSONFeatureCollection] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "features").write[Seq[GeoJSONFeature]]
  )(fc => ("FeatureCollection", fc.features))
  
}
  
