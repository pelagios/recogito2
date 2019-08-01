package controllers.document.downloads.serializers

import com.vividsolutions.jts.geom.Geometry
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.annotation.{Annotation, AnnotationBody}
import services.entity.EntityRecord

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