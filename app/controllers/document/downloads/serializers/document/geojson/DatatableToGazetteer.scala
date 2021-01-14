package controllers.document.downloads.serializers.document.geojson

import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory}
import controllers.HasCSVParsing
import controllers.document.downloads.FieldMapping
import controllers.document.downloads.serializers._
import java.io.File
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext
import scala.util.Try
import services.{HasGeometry, HasNullableSeq, ContentType}
import services.annotation.{Annotation, AnnotationBody, AnnotationService}
import services.document.ExtendedDocumentMetadata
import services.entity.{Entity, EntityRecord}
import services.entity.builtin.EntityService
import storage.uploads.Uploads

trait DatatableToGazetteer extends BaseSerializer with HasCSVParsing {

  private def findGazetteerRecords(annotation: Annotation, places: Seq[Entity]): Seq[EntityRecord] = {
    // Place URIs in this annotation
    val placeURIs = annotation.bodies
      .filter(_.hasType == AnnotationBody.PLACE)
      .flatMap(_.uri)
            
    placeURIs.flatMap { uri =>
      places.find(_.uris.contains(uri))
        .map(_.isConflationOf)
        .getOrElse(Seq.empty[EntityRecord])
        .filter(_.uri == uri)      
    }
  }

  def exportGeoJSONGazetteer(
    doc: ExtendedDocumentMetadata,
    fieldMapping: FieldMapping
  )(implicit annotationService: AnnotationService,
    entityService: EntityService, 
    uploads: Uploads,
    ctx: ExecutionContext) = exportMergedDocument(doc, { case (annotations, places, documentDir) =>
        
      val factory = new GeometryFactory()
      
      def toDouble(str: String) = 
        Try(str.trim().replace(",", ".").toDouble).toOption

      def rowToFeature(row: List[String], index: Int) = {
        val anchor = "row:" + index
        val maybeAnnotation = annotations.find(_.anchor == anchor)
        val matches = maybeAnnotation.map(annotation => findGazetteerRecords(annotation, places)).getOrElse(Seq.empty[EntityRecord])
        
        val id = row(fieldMapping.FIELD_ID)
        
        val geometry = (fieldMapping.FIELD_LATITUDE, fieldMapping.FIELD_LONGITUDE) match {
          case (Some(latIdx), Some(lonIdx)) =>
            val maybeLat = toDouble(row(latIdx))
            val maybeLon = toDouble(row(lonIdx))
            (maybeLat, maybeLon) match {
              case (Some(lat), Some(lon)) => Some(factory.createPoint(new Coordinate(lon, lat)))
              case _ => None
            }
            
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

object GazetteerRecordFeature extends HasGeometry with HasNullableSeq {
  
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