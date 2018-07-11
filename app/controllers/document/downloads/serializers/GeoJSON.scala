package controllers.document.downloads.serializers

import play.api.libs.json._
import play.api.libs.functional.syntax._

trait GeoJSONFeature

// TODO document metadata
case class GeoJSONFeatureCollection[T <: GeoJSONFeature](features: Seq[T])

object GeoJSONFeatureCollection {
  
  implicit def featureCollectionWrites[T <: GeoJSONFeature](implicit w: Writes[T]) = new Writes[GeoJSONFeatureCollection[T]] {
    def writes(fc: GeoJSONFeatureCollection[T]) = Json.obj(
      "type" -> "FeatureCollection",
      "features" -> Json.toJson(fc.features)
    )
  }

}