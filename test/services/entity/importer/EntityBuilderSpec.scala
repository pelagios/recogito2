package services.entity.importer

import com.vividsolutions.jts.geom.Geometry
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import scala.io.Source
import services.HasGeometry
import play.api.libs.json.JsObject

@RunWith(classOf[JUnitRunner])
class EntityBuilderSpec extends Specification with HasGeometry {
  
  val json = Source.fromFile("test/resources/services/entity/crosswalks/geonames_sample.json").getLines().mkString("\n")
  
  "The EntityBuilder" should {
    
    "build a proper centroid for a MultiPolygon" in {
      val geometry = (Json.parse(json) \\ "geometry").head
      val multipoly = Json.fromJson[Geometry](geometry).get
      
      val centroid = EntityBuilder.getCentroid(multipoly)

      centroid.x.isNaN must equalTo(false)
      centroid.y.isNaN must equalTo(false)
    }
    
  }
  
}