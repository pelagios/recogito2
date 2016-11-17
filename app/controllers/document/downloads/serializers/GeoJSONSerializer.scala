package controllers.document.downloads.serializers

import com.vividsolutions.jts.geom.Geometry
import models.HasGeometry
import models.annotation.{ Annotation, AnnotationBody, AnnotationService }
import models.place.{ GazetteerRecord, PlaceService }
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import storage.ES

trait GeoJSONSerializer extends BaseSerializer {

  def placesToGeoJSON(documentId: String)(implicit placeService: PlaceService, annotationService: AnnotationService, ctx: ExecutionContext) = {
    val fAnnotations = annotationService.findByDocId(documentId, 0, ES.MAX_SIZE)
    val fPlaces = placeService.listPlacesInDocument(documentId, 0, ES.MAX_SIZE)
    
    val f = for {
      annotations <- fAnnotations
      places <- fPlaces
    } yield (annotations.map(_._1), places)
    
    f.map { case (annotations, places) =>
      val placeAnnotations = annotations.filter(_.bodies.map(_.hasType).contains(AnnotationBody.PLACE))
        
      val features = places.items.flatMap { case (place, _) =>        
        val annotationsOnThisPlace = placeAnnotations.filter { a =>
          // All annotations that include place URIs of this place
          val placeURIs = a.bodies.filter(_.hasType == AnnotationBody.PLACE).flatMap(_.uri)
          !placeURIs.intersect(place.uris).isEmpty
        }
        
        val placeURIs = annotationsOnThisPlace.flatMap(_.bodies).filter(_.hasType == AnnotationBody.PLACE).flatMap(_.uri)
          
        place.representativeGeometry.map { geometry => 
          GeoJSONFeature(
            geometry,
            place.titles,
            // Keep only records explicitly referenced in the annotations
            place.isConflationOf.filter(g => placeURIs.contains(g.uri)),
            annotationsOnThisPlace
          )
        }
      }

      GeoJSONFeatureCollection(features)      
    }
  }

}

case class GeoJSONFeature(geometry: Geometry, titles: Seq[String], gazetteerRecords: Seq[GazetteerRecord], annotations: Seq[Annotation]) {
  
  private val bodies = annotations.flatMap(_.bodies)
  
  private def bodiesOfType(t: AnnotationBody.Type) = bodies.filter(_.hasType == t)
  
  val quotes = bodiesOfType(AnnotationBody.QUOTE).flatMap(_.value)
  
  val comments = bodiesOfType(AnnotationBody.COMMENT).flatMap(_.value)
  
  val tags = bodiesOfType(AnnotationBody.TAG).flatMap(_.value)
  
}

object GeoJSONFeature extends HasGeometry {
  
  implicit val geoJSONFeatureWrites: Writes[GeoJSONFeature] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "geometry").write[Geometry] and
    (JsPath \ "properties").write[JsObject] and
    (JsPath \ "uris").write[Seq[String]] and
    (JsPath \ "place_types").write[Seq[String]] and
    (JsPath \ "source_gazetteers").write[Seq[String]] and
    (JsPath \ "quotes").write[Seq[String]] and
    (JsPath \ "tags").write[Seq[String]] and
    (JsPath \ "comments").write[Seq[String]] 
  )(f => (
      "Feature",
      f.geometry,
      Json.obj(), // Empty properties object (for now)
      f.gazetteerRecords.map(_.uri),
      f.gazetteerRecords.flatMap(_.placeTypes),
      f.gazetteerRecords.map(_.sourceGazetteer.name),
      f.quotes,
      f.tags,
      f.comments
    )
  )
  
}

case class GeoJSONFeatureCollection(features: Seq[GeoJSONFeature])

object GeoJSONFeatureCollection {
  
  implicit val geoJSONFeatureCollectionWrites: Writes[GeoJSONFeatureCollection] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "features").write[Seq[GeoJSONFeature]]
  )(fc => ("FeatureCollection", fc.features))
  
}
  
