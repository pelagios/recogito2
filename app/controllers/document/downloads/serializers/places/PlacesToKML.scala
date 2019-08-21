package controllers.document.downloads.serializers.places

import com.vividsolutions.jts.geom.{Polygon, LineString}
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
        <name>{f.quotes.distinct.mkString(",")}</name>
        <ExtendedData>
          <Data name="Annotations">
            <value>{f.annotations.length}</value>
          </Data>
          <Data name="Place URIs">
            <value>{f.records.map(_.uri).mkString(", ")}</value>
          </Data>
          <Data name="Names (Gazetteer)">
            <value>{f.titles.mkString(",")}</value>
          </Data>
          <Data name="Toponyms (Document)">
            <value>{f.quotes.distinct.mkString(",")}</value>
          </Data>
          { if (!f.tags.isEmpty || !f.comments.isEmpty) {
            { if (!f.tags.isEmpty) { 
              <Data name="Tags">
                <value>{f.tags.mkString(", ")}</value>
              </Data>
            }}

            { if (!f.comments.isEmpty) {
              <Data name="comments">
                <value>{f.comments.mkString("\n\n")}</value>
              </Data>
            }}
          }}
        </ExtendedData>
        { f.geometry match {
          case geom: Polygon => 
            <Polygon>
              <extrude>1</extrude>
              <altitudeMode>clampToGround</altitudeMode>
              <outerBoundaryIs>
                <LinearRing>
                  <coordinates>
                    { geom.getCoordinates.map { coord => 
                      s"${coord.x},${coord.y},0\n"
                    }}
                  </coordinates>
                </LinearRing>
              </outerBoundaryIs>
            </Polygon>
          case geom =>
            <Point>
              <coordinates>{f.geometry.getCentroid.getX},{f.geometry.getCentroid.getY},0</coordinates>
            </Point>
        }}
      </Placemark>
    }

    <kml xmlns="http://www.opengis.net/kml/2.2">
      <Document>
        { kmlFeatures }
      </Document>
    </kml>
  } 

}