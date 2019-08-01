package controllers.document.downloads.serializers.places

import scala.concurrent.ExecutionContext
import services.annotation.{Annotation, AnnotationService, AnnotationBody}
import services.ContentType
import services.entity.{Entity, EntityType}
import services.entity.builtin.EntityService
import storage.es.ES

/** A KML serializer specific to the needs of the DARIAH GeoBrowser. 
  * 
  * The GeoBrowser is a bit special, as it needs every annotation 
  * represented as a separate Placemark. Also, in order to make use
  * of the GeoBrowser's animation functionality, we produce "fake timestamps"
  * based on annotation character order (only for plaintext documents).
  */
trait PlacesToGeoBrowser extends BaseGeoSerializer {

  private def getLabel(annotation: Annotation) = 
    annotation.bodies
      .filter(b => b.hasType == AnnotationBody.QUOTE || b.hasType == AnnotationBody.TRANSCRIPTION)
      .headOption
      .flatMap(_.value)
      .getOrElse("[missing quote]")

  private def toPlacemark(annotation: Annotation, places: Seq[Entity]) = {
    val coordinates = places.flatMap(_.representativePoint)

    val geom = if (coordinates.length == 1) {
      <Point>
        <coordinates>{coordinates(0).x},{coordinates(0).y},0</coordinates>
      </Point>
    } else {
      <MultiGeometry>
        { coordinates.map { coord => 
          <Point>
            <coordinates>{coord.x},{coord.y},0</coordinates>
          </Point>
        }}
      </MultiGeometry>
    }

    // TODO tags, comments

    val maybeTimestamp = annotation.annotates.contentType match {
      case ContentType.TEXT_PLAIN => 
        Some(<TimeStamp><when>{annotation.anchor.substring(12)}</when></TimeStamp>)

      case _ => None
    }

    <Placemark>
      <name>{ getLabel(annotation) }</name>
      <address>{ getLabel(annotation) }</address>
      { if (maybeTimestamp.isDefined) maybeTimestamp.get }
      { geom }
    </Placemark>
  }

  def placesToGeoBrowser(
    documentId: String
  )(implicit 
      entityService: EntityService, 
      annotationService: AnnotationService, 
      ctx: ExecutionContext
  ) = {
    val fAnnotations = annotationService.findByDocId(documentId, 0, ES.MAX_SIZE)
    val fPlaces = entityService.listEntitiesInDocument(documentId, Some(EntityType.PLACE), 0, ES.MAX_SIZE)
        
    val f = for {
      annotations <- fAnnotations
      places <- fPlaces
    } yield (annotations.map(_._1), places)
    
    f.map { case (annotations, p) =>
      // All places
      val places = p.items.map { p => p._1.entity }

      // All place annotations on this document
      val placeAnnotations = annotations.filter(_.bodies.map(_.hasType).contains(AnnotationBody.PLACE))  

      val placemarks = placeAnnotations.map { annotation =>
        val placeURIs = annotation.bodies.filter(_.hasType == AnnotationBody.PLACE).flatMap(_.uri)

        val placesForThisAnnotation = places.filter { case place => 
          // All places that match this annotation's place URIs 
          !place.uris.intersect(placeURIs).isEmpty
        }

        (annotation, placesForThisAnnotation)
      } map { t => toPlacemark(t._1, t._2) }

      <kml xmlns="http://www.opengis.net/kml/2.2">
        <Document>
          { placemarks }
        </Document>
      </kml>
    }
  } 

}