package services.entity.builtin

import com.vividsolutions.jts.geom.Geometry
import java.util.UUID
import org.specs2.mutable._
import org.specs2.runner._
import org.joda.time.DateTime
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import scala.io.Source
import services.HasGeometry
import services.entity._

@RunWith(classOf[JUnitRunner])
class EntityBuilderSpec extends Specification with HasGeometry {

  val json = Source.fromFile("test/resources/services/entity/crosswalks/geonames_sample.json").getLines().mkString("\n")

  lazy val multipoly = {
    val geom = (Json.parse(json) \\ "geometry").head
    Json.fromJson[Geometry](geom).get
  }

  "The EntityBuilder" should {

    "build a proper centroid for a MultiPolygon" in {
      val centroid = EntityBuilder.getCentroid(multipoly)
      centroid.x.isNaN must equalTo(false)
      centroid.y.isNaN must equalTo(false)
    }

    "create a proper entity from the sample record" in {
      val record = EntityRecord(
        "http://sws.geonames.org/1281843",
        "http://www.geonames.org",
        DateTime.now,
        None,
        "Vaavu Atholhu",
        Seq.empty[services.entity.Description],
        Seq(Name("Vaaf Atoll"), Name("Vaavu Atoll"), Name("Vāvu")),
        Some(multipoly),
        None, // representativePoint
        Some(CountryCode("MV")),
        None, // temporalBounds
        Seq.empty[String],
        None,
        Seq(
          Link("http://www.wikidata.org/wiki/Q2709111", LinkType.CLOSE_MATCH),
          Link("http://en.wikipedia.org/wiki/Vaavu_Atoll", LinkType.CLOSE_MATCH)
        ))

      val entity = EntityBuilder.fromRecords(Seq(record), EntityType.PLACE, UUID.randomUUID)
      entity.representativePoint.isDefined must equalTo(true)
    }

  }

}
