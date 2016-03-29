package models.place

import com.vividsolutions.jts.geom.{ Coordinate, GeometryFactory }
import org.specs2.mutable._
import org.specs2.runner._
import org.joda.time.{ DateTime, DateTimeZone }
import org.junit.runner._
import play.api.Logger
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class PlaceSpec extends Specification {
  
  "sample place" should {

    val json = Source.fromFile("test/resources/place.json").getLines().mkString("\n")
    val parseResult = Json.fromJson[Place](Json.parse(json))
    
    "be properly created from JSON" in {
      
      parseResult.isSuccess must equalTo(true)
    }
    
    "have the expected top-level properties (ID, title, geometry, temporal bounds)" in {
      val place = parseResult.get
      
      place.id must equalTo ("http://pleiades.stoa.org/places/118543")
      place.title must equalTo("Ad Mauros")

      val location = new Coordinate(14.02358, 48.31058)
      place.representativePoint must equalTo(Some(location))
      place.geometry must equalTo(Some(new GeometryFactory().createPoint(location)))

      val from = new DateTime(DateTimeZone.UTC).withDate(-30, 1, 1).withTime(0, 0, 0, 0)
      val to = new DateTime(DateTimeZone.UTC).withDate(640, 1, 1).withTime(0, 0, 0, 0)
      place.temporalBounds must equalTo(Some(TemporalBounds(from, to)))
    }
    
    "should report the expected gazetteer record URIs" in {
      val place = parseResult.get
      val expectedURIs = Seq(
          "http://pleiades.stoa.org/places/118543",
          "http://dare.ht.lu.se/places/10778",
          "http://www.trismegistos.org/place/35191")    
          
      place.uris.size must equalTo(3)
      place.uris must containAllOf(expectedURIs)
    }
    
    "should report the expected source gazetteers" in {
      val place = parseResult.get
      val expectedGazetteers = 
        Seq("Pleiades", "Trismegistos", "DARE").map(Gazetteer(_))
        
      place.isInGazetteers.size must equalTo(3)
      place.isInGazetteers must containAllOf(expectedGazetteers)
    }
    
    "should list the expected place types per gazetteer" in {
      val place = parseResult.get      
      
      place.placeTypes.size must equalTo(2)
      place.placeTypes.get("fort").get must containAllOf(Seq(Gazetteer("DARE"), Gazetteer("Pleiades")))
      place.placeTypes.get("tower").get must equalTo(Seq(Gazetteer("Pleiades")))
    }
    
    "should list the expected descriptions by gazetteer" in {
      val place = parseResult.get

      place.descriptions.size must equalTo(1)
      place.descriptions.head._1 must equalTo(Description("An ancient place, cited: BAtlas 12 H4 Ad Mauros"))
      place.descriptions.head._2 must equalTo(Seq(Gazetteer("Pleiades")))
    }
    
    "should list the expected names per gazetteer" in {
      val place = parseResult.get
      
      place.names.size must equalTo(4)
      place.names.get(Name("Ad Mauros")).get must containAllOf(Seq(Gazetteer("Trismegistos"), Gazetteer("Pleiades")))
      place.names.get(Name("Ad Mauros/Marinianio, Eferding")).get must equalTo(Seq(Gazetteer("DARE")))
      place.names.get(Name("Eferding")).get must equalTo(Seq(Gazetteer("Trismegistos")))
      place.names.get(Name("Marianianio", Some("la"))).get must equalTo(Seq(Gazetteer("Trismegistos")))      
    }
    
    "should list the expected close- and exactMatches" in {
      val place = parseResult.get
      val expectedCloseMatches = Seq(
        "http://sws.geonames.org/2780394",
        "http://www.wikidata.org/entity/Q2739862",
        "http://de.wikipedia.org/wiki/Kastell_Eferding",
        "http://www.cambridge.org/us/talbert/talbertdatabase/TPPlace1513.html")
       
      place.closeMatches.size must equalTo(4)
      place.closeMatches must containAllOf(expectedCloseMatches)
      
      place.exactMatches.size must equalTo(0)
      
      place.allMatches.size must equalTo(4)
      place.allMatches must containAllOf(expectedCloseMatches)
    }
    
  }
    
  "JSON serialization/parsing roundtrip" should {
    
    "yield an equal Place" in {
      
      import GazetteerRecordSpec._
      
      val before = Place(
        "http://pleiades.stoa.org/places/118543",
        "Ad Mauros",
        dareRecord.geometry,
        dareRecord.representativePoint,
        dareRecord.temporalBounds,
        Seq(pleiadesRecord, dareRecord, trismegistosRecord)
      )
                  
      val serializedToJson = Json.stringify(Json.toJson(before))
      val parsedFromJson = Json.fromJson[Place](Json.parse(serializedToJson))
      parsedFromJson.isSuccess must equalTo(true)
      
      val after = parsedFromJson.get
      after must equalTo(before)
    }
    
  }
  
  // TODO test index insertion (i.e. does the ES mapping definition fit the test JSON? 
    
}