package services.entity

import java.util.UUID
import com.vividsolutions.jts.geom.{ Coordinate, GeometryFactory }
import org.specs2.mutable._
import org.specs2.runner._
import org.joda.time.{ DateTime, DateTimeZone }
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class PlaceSpec extends Specification {

  "The sample place" should {

    val json = Source.fromFile("test/resources/services/entity/place.json").getLines().mkString("\n")
    val parseResult = Json.fromJson[Entity](Json.parse(json))
    
    "be properly created from JSON" in {
      parseResult.isSuccess must equalTo(true)
    }
    
    "have the expected top-level properties (ID, title, geometry, temporal bounds)" in {
      val place = parseResult.get
      
      place.unionId must equalTo (UUID.fromString("69d6b969-7f6b-4934-8c81-848dd194aeaf"))
      place.titles must contain("Ad Mauros")

      val location = new Coordinate(14.02358, 48.31058)
      place.representativePoint must equalTo(Some(location))
      place.representativeGeometry must equalTo(Some(new GeometryFactory().createPoint(location)))
      place.temporalBoundsUnion must equalTo(Some(TemporalBounds.fromYears(-30, 640)))
    }
    
    "report the expected gazetteer record URIs" in {
      val place = parseResult.get
      val expectedURIs = Seq(
          "http://pleiades.stoa.org/places/118543",
          "http://dare.ht.lu.se/places/10778",
          "http://www.trismegistos.org/place/35191")    
          
      place.uris.size must equalTo(3)
      place.uris must containAllOf(expectedURIs)
    }
    
    "report the expected source gazetteers" in {
      val place = parseResult.get
      val expectedGazetteers = Seq("http://pleiades.stoa.org", "http://www.trismegistos.org", "http://dare.ht.lu.se/")
        
      place.sourceAuthorities.size must equalTo(3)
      place.sourceAuthorities must containAllOf(expectedGazetteers)
    }
    
    "list the expected place types per gazetteer" in {
      val place = parseResult.get      
      
      place.subjects.size must equalTo(2)
      place.subjects.get("fort").get must containAllOf(Seq("http://dare.ht.lu.se/", "http://pleiades.stoa.org"))
      place.subjects.get("tower").get must equalTo(Seq("http://pleiades.stoa.org"))
    }
    
    "list the expected descriptions by gazetteer" in {
      val place = parseResult.get

      place.descriptions.size must equalTo(1)
      place.descriptions.head._1 must equalTo(Description("An ancient place, cited: BAtlas 12 H4 Ad Mauros"))
      place.descriptions.head._2 must equalTo(Seq("http://pleiades.stoa.org"))
    }
    
    "list the expected names per gazetteer" in {
      val place = parseResult.get
      
      place.names.size must equalTo(4)
      place.names.get(Name("Ad Mauros")).get must containAllOf(Seq("http://www.trismegistos.org", "http://pleiades.stoa.org"))
      place.names.get(Name("Ad Mauros/Marinianio, Eferding")).get must equalTo(Seq("http://dare.ht.lu.se/"))
      place.names.get(Name("Eferding")).get must equalTo(Seq("http://www.trismegistos.org"))
      place.names.get(Name("Marianianio", Some("la"))).get must equalTo(Seq("http://www.trismegistos.org"))      
    }
    
    "list the expected close- and exactMatches" in {
      val place = parseResult.get
      val expectedCloseMatches = Seq(
        Link("http://sws.geonames.org/2780394", LinkType.CLOSE_MATCH),
        Link("http://www.wikidata.org/entity/Q2739862", LinkType.CLOSE_MATCH),
        Link("http://de.wikipedia.org/wiki/Kastell_Eferding", LinkType.CLOSE_MATCH),
        Link("http://www.cambridge.org/us/talbert/talbertdatabase/TPPlace1513.html", LinkType.CLOSE_MATCH))
        
      place.links.size must equalTo(4)       
      place.links must containAllOf(expectedCloseMatches)      
    }
    
  }
    
  "A JSON serialization/parsing roundtrip" should {
    
    "yield an equal Place before and after" in {
      
      import EntityRecordSpec._
      
      val before = Entity(
        UUID.randomUUID,
        EntityType.PLACE,
        "Ad Mauros",
        dareRecord.geometry,
        dareRecord.representativePoint,
        dareRecord.temporalBounds,
        Seq(pleiadesRecord, dareRecord, trismegistosRecord)
      )
                  
      val serializedToJson = Json.stringify(Json.toJson(before))
      
      val parsedFromJson = Json.fromJson[Entity](Json.parse(serializedToJson))
      parsedFromJson.isSuccess must equalTo(true)
      
      val after = parsedFromJson.get
      // Per design, the BBox field is filled during the roundtrip - ignore it for the comparison!
      val afterWithoutBBox = after.copy(storedBBox = None)
      afterWithoutBBox must equalTo(before)
    }
    
  }
       
}