package controllers.document.downloads.serializers.places

import com.vividsolutions.jts.geom.{Coordinate, Geometry}
import controllers.HasCSVParsing
import controllers.document.downloads.FieldMapping
import controllers.document.downloads.serializers._
import java.io.File
import org.geotools.geometry.jts.JTSFactoryFinder
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext
import scala.util.Try
import services.{ContentType, HasGeometry, HasNullableSeq}
import services.annotation.{Annotation, AnnotationBody, AnnotationService}
import services.document.DocumentInfo
import services.entity.{Entity, EntityRecord, EntityType}
import services.entity.builtin.EntityService
import storage.es.ES 
import storage.uploads.Uploads

trait GeoJSONSerializer extends BaseSerializer with HasCSVParsing {
  
  def placesToGeoJSON(documentId: String)(implicit entityService: EntityService, annotationService: AnnotationService, ctx: ExecutionContext) = {
    val fAnnotations = annotationService.findByDocId(documentId, 0, ES.MAX_SIZE)
    val fPlaces = entityService.listEntitiesInDocument(documentId, Some(EntityType.PLACE), 0, ES.MAX_SIZE)
        
    val f = for {
      annotations <- fAnnotations
      places <- fPlaces
    } yield (annotations.map(_._1), places)
    
    f.map { case (annotations, places) =>
      val placeAnnotations = annotations.filter(_.bodies.map(_.hasType).contains(AnnotationBody.PLACE))      
      val features = places.items.flatMap { e =>      
        val place = e._1.entity
        val annotationsOnThisPlace = placeAnnotations.filter { a =>
          // All annotations that include place URIs of this place
          val placeURIs = a.bodies.filter(_.hasType == AnnotationBody.PLACE).flatMap(_.uri)
          !placeURIs.intersect(place.uris).isEmpty
        }
        
        val placeURIs = annotationsOnThisPlace.flatMap(_.bodies).filter(_.hasType == AnnotationBody.PLACE).flatMap(_.uri)
        val referencedRecords = place.isConflationOf.filter(g => placeURIs.contains(g.uri))
        
        place.representativeGeometry.map(geometry => 
          ReferencedPlaceFeature(geometry, referencedRecords, annotationsOnThisPlace))
      }

      Json.toJson(GeoJSONFeatureCollection(features))
    }
  }
    
}

/** Feature representing references to a place in a document **/ 
case class ReferencedPlaceFeature(
  geometry         : Geometry,
  gazetteerRecords : Seq[EntityRecord],
  annotations      : Seq[Annotation]
) extends GeoJSONFeature {
  
  private val bodies = annotations.flatMap(_.bodies) 
  private def bodiesOfType(t: AnnotationBody.Type) = bodies.filter(_.hasType == t)

  val titles = gazetteerRecords.map(_.title).distinct
  val quotes = bodiesOfType(AnnotationBody.QUOTE).flatMap(_.value)
  val comments = bodiesOfType(AnnotationBody.COMMENT).flatMap(_.value)
  val tags = bodiesOfType(AnnotationBody.TAG).flatMap(_.value)
  
}

object ReferencedPlaceFeature extends HasGeometry with HasNullableSeq {

  implicit val referencedPlaceFeatureWrites: Writes[ReferencedPlaceFeature] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "geometry").write[Geometry] and
    (JsPath \ "properties").write[JsObject] and
    (JsPath \ "uris").write[Seq[String]] and
    (JsPath \ "titles").write[Seq[String]] and
    (JsPath \ "names").writeNullable[Seq[String]] and
    (JsPath \ "place_types").writeNullable[Seq[String]] and
    (JsPath \ "source_gazetteers").write[Seq[String]] and
    (JsPath \ "quotes").writeNullable[Seq[String]] and
    (JsPath \ "tags").writeNullable[Seq[String]] and
    (JsPath \ "comments").writeNullable[Seq[String]] 
  )(f => (
      "Feature",
      f.geometry,
      Json.obj(
        "titles" -> f.titles.mkString(", "),
        "annotations" -> f.annotations.size
      ),
      f.gazetteerRecords.map(_.uri),
      f.gazetteerRecords.map(_.title),
      toOptSeq(f.gazetteerRecords.flatMap(_.names.map(_.name))),
      toOptSeq(f.gazetteerRecords.flatMap(_.subjects)),
      f.gazetteerRecords.map(_.sourceAuthority),
      toOptSeq(f.quotes),
      toOptSeq(f.tags),
      toOptSeq(f.comments)
    )
  )
  
}


  
