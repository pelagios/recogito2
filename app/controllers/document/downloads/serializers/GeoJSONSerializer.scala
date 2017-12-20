package controllers.document.downloads.serializers

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import controllers.HasCSVParsing
import controllers.document.downloads.FieldMapping
import java.io.File
import models.{ ContentType, HasGeometry }
import models.annotation.{ Annotation, AnnotationBody, AnnotationService }
import models.document.DocumentInfo
import models.place.{ Place, PlaceService, GazetteerRecord }
import org.geotools.geometry.jts.JTSFactoryFinder
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext
import storage.{ ES, Uploads }

trait GeoJSONSerializer extends BaseSerializer with HasCSVParsing {
  
  private def findGazetteerRecords(annotation: Annotation, places: Seq[Place]): Seq[GazetteerRecord] = {
    // Place URIs in this annotation
    val placeURIs = annotation.bodies
      .filter(_.hasType == AnnotationBody.PLACE)
      .flatMap(_.uri)
            
    placeURIs.flatMap { uri =>
      places.find(_.uris.contains(uri))
        .map(_.isConflationOf)
        .getOrElse(Seq.empty[GazetteerRecord])
        .filter(_.uri == uri)      
    }
  }

  def placesToGeoJSON(documentId: String)(implicit placeService: PlaceService, annotationService: AnnotationService, ctx: ExecutionContext) = {
    val fAnnotations = annotationService.findByDocId(documentId, 0, ES.MAX_SIZE)
    val fPlaces = placeService.listPlacesInDocument(documentId, 0, ES.MAX_SIZE)
        
    val f = for {
      annotations <- fAnnotations
      places <- fPlaces
    } yield (annotations.map(_._1), places)
    
    f.map { case (annotations, places) =>
      
      play.api.Logger.info("Places: " + places.total + " - " + places.items.size)
      play.api.Logger.info("Annotations: " + annotations.size)
      
      val placeAnnotations = annotations.filter(_.bodies.map(_.hasType).contains(AnnotationBody.PLACE))
      val features = places.items.flatMap { case (place, _) =>        
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
  
  def exportGeoJSONGazetteer(
      doc: DocumentInfo,
      fieldMapping: FieldMapping
  )(implicit annotationService: AnnotationService,
      placeService: PlaceService, 
      uploads: Uploads,
      ctx: ExecutionContext) = exportMergedDocument(doc, { case (annotations, places, documentDir) =>
        
      val factory = JTSFactoryFinder.getGeometryFactory()
      
      def toDouble(str: String) = str.trim().replace(",", ".").toDouble

      def rowToFeature(row: List[String], index: Int) = {
        val anchor = "row:" + index
        val maybeAnnotation = annotations.find(_.anchor == anchor)
        val matches = maybeAnnotation.map(annotation => findGazetteerRecords(annotation, places)).getOrElse(Seq.empty[GazetteerRecord])
        
        val id = row(fieldMapping.FIELD_ID)
        
        val geometry = (fieldMapping.FIELD_LATITUDE, fieldMapping.FIELD_LONGITUDE) match {
          case (Some(lat), Some(lon)) =>
            Some(factory.createPoint(new Coordinate(toDouble(row(lon)), toDouble(row(lat)))))
            
          case _ => None
        }
        
        GazetteerRecordFeature(
          id,
          fieldMapping.BASE_URL.getOrElse("http://www.example.com/") + id,
          row(fieldMapping.FIELD_TITLE),
          fieldMapping.FIELDS_NAME.map(idx => Seq(row(idx))).getOrElse(Seq.empty[String]),
          geometry,
          fieldMapping.FIELD_DESCRIPTION.map(row(_)),
          fieldMapping.FIELD_COUNTRY.map(row(_)),
          matches.map(_.uri)
        )
      }
      
      val tables =
        doc.fileparts
          .withFilter(part => ContentType.withName(part.getContentType).map(_.isData).getOrElse(false))
          .map(part => (part, new File(documentDir, part.getFile)))
          
      val features = tables.flatMap { case (part, file) =>
        parseCSV(file, guessDelimiter(file), header = true, { case (row, idx) =>
          rowToFeature(row, idx) }).toList.flatten
      }
      
      Json.toJson(GeoJSONFeatureCollection(features))
  })
  
}

sealed trait GeoJSONFeature

object GeoJSONFeature extends HasGeometry {
  
  def toOptSeq[T](s: Seq[T]) = if (s.isEmpty) None else Some(s)
  
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
      toOptSeq(f.gazetteerRecords.flatMap(_.placeTypes)),
      f.gazetteerRecords.map(_.sourceGazetteer.name),
      toOptSeq(f.quotes),
      toOptSeq(f.tags),
      toOptSeq(f.comments)
    )
  )
  
  implicit val gazetteerRecordFeatureWrites: Writes[GazetteerRecordFeature] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "id").write[String] and
    (JsPath \ "uri").write[String] and
    (JsPath \ "title").write[String] and
    (JsPath \ "geometry").writeNullable[Geometry] and
    (JsPath \ "properties").write[JsObject] and
    (JsPath \ "names").writeNullable[Seq[JsObject]] and
    (JsPath \ "links").writeNullable[JsObject]
  )(f => (
      "Feature",
      f.id,
      f.uri,
      f.title,
      f.geometry,
      Json.obj(),
      toOptSeq(f.names.map(name => Json.obj("name" -> name))),
      {
        if (f.closeMatches.isEmpty) None
        else Some(Json.obj("close_matches" -> f.closeMatches))
      }
    )
  )
  
}

/** Feature representing references to a place in a document **/ 
case class ReferencedPlaceFeature(
  geometry         : Geometry,
  gazetteerRecords : Seq[GazetteerRecord],
  annotations      : Seq[Annotation]
) extends GeoJSONFeature {
  
  private val bodies = annotations.flatMap(_.bodies) 
  private def bodiesOfType(t: AnnotationBody.Type) = bodies.filter(_.hasType == t)

  val titles = gazetteerRecords.map(_.title).distinct
  val quotes = bodiesOfType(AnnotationBody.QUOTE).flatMap(_.value)
  val comments = bodiesOfType(AnnotationBody.COMMENT).flatMap(_.value)
  val tags = bodiesOfType(AnnotationBody.TAG).flatMap(_.value)
  
}

/** Feature representing a CSV gazetteer record **/
case class GazetteerRecordFeature(
  id           : String,
  uri          : String,
  title        : String,
  names        : Seq[String],
  geometry     : Option[Geometry],
  description  : Option[String],
  countryCode  : Option[String],
  closeMatches : Seq[String]
) extends GeoJSONFeature

// TODO document metadata
case class GeoJSONFeatureCollection[T <: GeoJSONFeature](features: Seq[T])

object GeoJSONFeatureCollection {
  
  implicit val referencedPlaceCollectionWrites: Writes[GeoJSONFeatureCollection[ReferencedPlaceFeature]] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "features").write[Seq[ReferencedPlaceFeature]]
  )(fc => ("FeatureCollection", fc.features))
  
  implicit val gazetteerRecordCollectionWrites: Writes[GeoJSONFeatureCollection[GazetteerRecordFeature]] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "features").write[Seq[GazetteerRecordFeature]]
  )(fc => ("FeatureCollection", fc.features))
  
}
  
