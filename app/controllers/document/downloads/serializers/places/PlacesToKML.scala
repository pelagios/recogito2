package controllers.document.downloads.serializers.places

import scala.concurrent.ExecutionContext
import services.annotation.{AnnotationService, AnnotationBody}
import services.entity.EntityType
import services.entity.builtin.EntityService
import storage.es.ES

trait PlacesToKML extends BaseGeoSerializer {

  def placesToKML(
    documentId: String
  )(implicit 
      entityService: EntityService, 
      annotationService: AnnotationService, 
      ctx: ExecutionContext
  ) = getMappableFeatures(documentId).map { features => 

    val kmlFeatures = features.map { f => 
      <Placemark>
        <name>{ f.records.map(_.title).distinct }</name>
        <Point>
          <coordinates>{f.geometry.getCentroid.getX},{f.geometry.getCentroid.getY},0</coordinates>
        </Point>
      </Placemark>
    }

    <kml xmlns="http://www.opengis.net/kml/2.2">
      <Document>
        { kmlFeatures }
      </Document>
    </kml>
  } 

}