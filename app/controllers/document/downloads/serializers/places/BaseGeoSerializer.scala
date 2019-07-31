package controllers.document.downloads.serializers.places

import com.vividsolutions.jts.geom.Geometry
import controllers.document.downloads.serializers.BaseSerializer
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.{Annotation, AnnotationService, AnnotationBody}
import services.entity.{EntityType, EntityRecord}
import services.entity.builtin.EntityService
import storage.es.ES

case class BaseFeature(
  geometry: Geometry,
  records: Seq[EntityRecord],
  annotations: Seq[Annotation])

trait BaseGeoSerializer extends BaseSerializer {

  def getMappableFeatures(
    documentId: String
  )(implicit 
      entityService: EntityService, 
      annotationService: AnnotationService, 
      ctx: ExecutionContext
  ): Future[Seq[BaseFeature]] = {
    val fAnnotations = annotationService.findByDocId(documentId, 0, ES.MAX_SIZE)
    val fPlaces = entityService.listEntitiesInDocument(documentId, Some(EntityType.PLACE), 0, ES.MAX_SIZE)
        
    val f = for {
      annotations <- fAnnotations
      places <- fPlaces
    } yield (annotations.map(_._1), places)
    
    f.map { case (annotations, places) =>
      // All place annotations on this document
      val placeAnnotations = annotations.filter(_.bodies.map(_.hasType).contains(AnnotationBody.PLACE))  

      // Each place in this document, along with all the annotations on this place and 
      // the specific entity records the annotations point to (within the place union record) 
      places.items.flatMap { e =>      
        val place = e._1.entity

        val annotationsOnThisPlace = placeAnnotations.filter { a =>
          // All annotations that include place URIs of this place
          val placeURIs = a.bodies.filter(_.hasType == AnnotationBody.PLACE).flatMap(_.uri)
          !placeURIs.intersect(place.uris).isEmpty
        }

        val placeURIs = annotationsOnThisPlace.flatMap(_.bodies).filter(_.hasType == AnnotationBody.PLACE).flatMap(_.uri)
        val referencedRecords = place.isConflationOf.filter(g => placeURIs.contains(g.uri))
        
        place.representativeGeometry.map { geom => 
          BaseFeature(geom, referencedRecords, annotationsOnThisPlace)
        }
      }
    }        
  }

}