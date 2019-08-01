package controllers.document.downloads.serializers.places

import controllers.HasTextSnippets
import controllers.document.downloads.serializers.BaseSerializer
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.{Annotation, AnnotationService, AnnotationBody}
import services.ContentType
import services.document.ExtendedDocumentMetadata
import services.entity.{Entity, EntityType}
import services.entity.builtin.EntityService
import storage.es.ES
import storage.uploads.Uploads

/** A KML serializer specific to the needs of the DARIAH GeoBrowser. 
  * 
  * The GeoBrowser is a bit special, as it needs every annotation 
  * represented as a separate Placemark. Also, in order to make use
  * of the GeoBrowser's animation functionality, we produce "fake timestamps"
  * based on annotation character order (only for plaintext documents).
  */
trait PlacesToGeoBrowser extends BaseSerializer with HasTextSnippets {

  private def getLabel(annotation: Annotation) = 
    annotation.bodies
      .filter(b => b.hasType == AnnotationBody.QUOTE || b.hasType == AnnotationBody.TRANSCRIPTION)
      .headOption
      .flatMap(_.value)
      .getOrElse("[missing quote]")

  private def toPlacemark(annotation: Annotation, places: Seq[Entity], plaintexts: Seq[(UUID, String)]) = {
    val snippet = plaintexts.find(_._1 == annotation.annotates.filepartId).map { case (id, text) => 
      snippetFromText(text, annotation)
    }

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

    <Placemark>
      <name>{ getLabel(annotation) }</name>
      { if (snippet.isDefined) <description>{ snippet.get.text } </description> }
      <address>{ getLabel(annotation) }</address>
      { if (snippet.isDefined) <TimeStamp><when>{ snippet.get.offset }</when></TimeStamp> }
      { geom }
    </Placemark>
  }

  def placesToGeoBrowser(
    documentId: String,
    doc: ExtendedDocumentMetadata
  )(implicit 
      entityService: EntityService, 
      annotationService: AnnotationService, 
      uploads: Uploads,
      ctx: ExecutionContext
  ) = {
    val fAnnotations = annotationService.findByDocId(documentId, 0, ES.MAX_SIZE)
    val fPlaces = entityService.listEntitiesInDocument(documentId, Some(EntityType.PLACE), 0, ES.MAX_SIZE)
    val fPlaintexts = Future.sequence {
      doc.fileparts.filter(_.getContentType == ContentType.TEXT_PLAIN.toString).map { part => 
        uploads.readTextfile(doc.owner.getUsername, doc.id, part.getFile).map { maybeText =>
          maybeText.map((part.getId, _))
        }
      }
    }
        
    val f = for {
      annotations <- fAnnotations
      places <- fPlaces
      plaintexts <- fPlaintexts
    } yield (annotations.map(_._1), places, plaintexts.flatten)
    
    f.map { case (annotations, p, plaintexts) =>
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
      } map { t => toPlacemark(t._1, t._2, plaintexts) }

      <kml xmlns="http://www.opengis.net/kml/2.2">
        <Document>
          { placemarks }
        </Document>
      </kml>
    }
  } 

}