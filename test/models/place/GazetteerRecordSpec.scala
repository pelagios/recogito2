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
class GazetteerRecordSpec extends Specification {

  "sample gazetteer records" should {
    
    "be properly created from place JSON" in {
      val json = Source.fromFile("test/resources/place.json").getLines().mkString("\n")
      val parseResult = Json.fromJson[Place](Json.parse(json))
      
      parseResult.isSuccess must equalTo(true)
      
      val gazetteerRecords = parseResult.get.isConflationOf
      
      gazetteerRecords.size must equalTo(3)
      
      val coord = new Coordinate(14.02358, 48.31058)
      val point = new GeometryFactory().createPoint(coord)
      
      val from = new DateTime(DateTimeZone.UTC).withDate(-30, 1, 1).withTime(0, 0, 0, 0)
      val to = new DateTime(DateTimeZone.UTC).withDate(640, 1, 1).withTime(0, 0, 0, 0)

      val pleiadesRecord = GazetteerRecord(
        "http://pleiades.stoa.org/places/118543",
        Gazetteer("Pleiades"),
        "Ad Mauros",
        Seq("fort" , "tower"),
        Seq(Description("An ancient place, cited: BAtlas 12 H4 Ad Mauros")),
        Seq(Name("Ad Mauros")),
        Some(point),
        Some(coord),
        Some(TemporalBounds(from, to)),
        Seq.empty[String],
        Seq.empty[String])
        
      val dareRecord = GazetteerRecord(
        "http://dare.ht.lu.se/places/10778",
        Gazetteer("DARE"),
        "Ad Mauros/Marinianio, Eferding",
        Seq("fort"),
        Seq.empty[Description],
        Seq(Name("Ad Mauros/Marinianio, Eferding")),
        Some(point),
        Some(coord),
        Some(TemporalBounds(from, to)),
        Seq(
          "http://sws.geonames.org/2780394",
          "http://www.wikidata.org/entity/Q2739862",
          "http://de.wikipedia.org/wiki/Kastell_Eferding",
          "http://www.cambridge.org/us/talbert/talbertdatabase/TPPlace1513.html"
        ),
        Seq.empty[String]) 
        
      val trismegistosRecord = GazetteerRecord(
        "http://www.trismegistos.org/place/35191",
        Gazetteer("Trismegistos"),
        "Ad Mauros",
        Seq.empty[String],
        Seq.empty[Description],
        Seq(
          Name("Ad Mauros"),
          Name("Eferding"),
          Name("Marianianio", Some("la"))
        ),
        None,
        None,
        None,
        Seq.empty[String],
        Seq.empty[String])
        
      gazetteerRecords must containAllOf(Seq(pleiadesRecord, dareRecord, trismegistosRecord))
    }
    
  }
  
  "the flex date parser" should {
    
    "parse integer- and datestring-formatted years as equal DateTimes" in {
      
      val jsonTempBoundsInt = Json.parse("{ \"from\": -30, \"to\": 640 }")      
      val jsonTempBoundsStr = Json.parse("{ \"from\": \"-30-01-01T00:00:00Z\", \"to\": \"640-01-01T00:00:00Z\" }") 

      val boundsFromInt = Json.fromJson[TemporalBounds](jsonTempBoundsInt)
      val boundsFromStr = Json.fromJson[TemporalBounds](jsonTempBoundsStr)

      boundsFromInt.isSuccess must equalTo(true)
      boundsFromStr.isSuccess must equalTo(true)
      
      boundsFromStr.get.from.getYear must equalTo(-30)
      boundsFromStr.get.to.getYear must equalTo(640)
      
      boundsFromInt.get.from.getMillis must equalTo(boundsFromStr.get.from.getMillis)
      boundsFromInt.get.to.getMillis must equalTo(boundsFromStr.get.to.getMillis)
    }
    
  }
  
  "temp bounds (de)serialization" should {
    
    "yield dates serialized in UTC" in  {
      val tempBounds = TemporalBounds(
          new DateTime(DateTimeZone.UTC).withDate(1492, 1, 1).withTime(0, 0, 0, 0),
          new DateTime(DateTimeZone.UTC).withDate(1493, 1, 1).withTime(0, 0, 0, 0))
      
      val asJson = Json.toJson(tempBounds)
      
      (asJson \ "from").as[String] must equalTo ("1492-01-01T00:00:00+00:00")
      (asJson \ "to").as[String] must equalTo ("1493-01-01T00:00:00+00:00")
    }
    
    "maintain UTC in a parse/serialize roundtrip" in {
      val json = "{\"from\":\"1492-01-01T00:00:00+00:00\",\"to\":\"1493-01-01T00:00:00+00:00\"}"
      
      val asDateTime = Json.fromJson[TemporalBounds](Json.parse(json))
      asDateTime.isSuccess must equalTo(true)
      
      val serialized = Json.toJson(asDateTime.get)
      Json.stringify(serialized) must equalTo(json)
    }
    
  }
  
}