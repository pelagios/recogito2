package services.entity.builtin.importer.crosswalks.geojson

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class GeoNamesCrosswalkSpec extends Specification {

  val json = Source.fromFile("test/resources/services/entity/crosswalks/geonames_sample.json").getLines().mkString("\n")

  "The sample GeoNames record" should {

    "be properly parsed" in {
      val record = Json.fromJson[GeoNamesRecord](Json.parse(json))
      record.isSuccess must equalTo(true)

      val geom = record.get.features.map(_.geometry).head
      record.get.representativePoint.isDefined must equalTo(false)
      record.get.features.size must equalTo(1)
    }

  }

}
