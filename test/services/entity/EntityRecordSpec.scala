package services.entity

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory}
import org.specs2.mutable._
import org.specs2.runner._
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import scala.io.Source

object EntityRecordSpec {
  
  private val DATE_TIME_PATTERN = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
  
  private val SYNC_TIME = DateTime.parse("2016-04-03T11:23:00Z", DATE_TIME_PATTERN).withZone(DateTimeZone.UTC)
  
  private val coord = new Coordinate(14.02358, 48.31058)
  private val point = new GeometryFactory().createPoint(coord)
      
  private val from = new DateTime(DateTimeZone.UTC).withDate(-30, 1, 1).withTime(0, 0, 0, 0)
  private val to = new DateTime(DateTimeZone.UTC).withDate(640, 1, 1).withTime(0, 0, 0, 0)
  
  val pleiadesRecord = EntityRecord(
    "http://pleiades.stoa.org/places/118543",
    "Pleiades",
    SYNC_TIME,
    Some(SYNC_TIME),
    "Ad Mauros",
    Seq(Description("An ancient place, cited: BAtlas 12 H4 Ad Mauros")),
    Seq(Name("Ad Mauros")),
    Some(point),
    Some(coord),
    None,
    Some(TemporalBounds(from, to)),
    Seq("fort" , "tower"),
    None,
    Seq.empty[Link])
    
  val dareRecord = EntityRecord(
    "http://dare.ht.lu.se/places/10778",
    "DARE",
    SYNC_TIME,
    Some(SYNC_TIME),
    "Ad Mauros/Marinianio, Eferding",
    Seq.empty[Description],
    Seq(Name("Ad Mauros/Marinianio, Eferding")),
    Some(point),
    Some(coord),
    None,
    Some(TemporalBounds(from, to)),
    Seq("fort"),
    None,
    Seq(
      Link("http://sws.geonames.org/2780394", LinkType.CLOSE_MATCH),
      Link("http://www.wikidata.org/entity/Q2739862", LinkType.CLOSE_MATCH),
      Link("http://de.wikipedia.org/wiki/Kastell_Eferding", LinkType.CLOSE_MATCH),
      Link("http://www.cambridge.org/us/talbert/talbertdatabase/TPPlace1513.html", LinkType.CLOSE_MATCH)
    )) 
    
  val trismegistosRecord = EntityRecord(
    "http://www.trismegistos.org/place/35191",
    "Trismegistos",
    SYNC_TIME,
    None,
    "Ad Mauros",
    Seq.empty[Description],
    Seq(
      Name("Ad Mauros"),
      Name("Eferding"),
      Name("Marianianio", Some("la"))
    ),
    None,
    None,
    None,
    None,
    Seq.empty[String],
    None,
    Seq.empty[Link])
  
}

@RunWith(classOf[JUnitRunner])
class EntityRecordSpec extends Specification {

  import EntityRecordSpec._
  
  "sample gazetteer records" should {
    
    "be properly created from place JSON" in {
      val json = Source.fromFile("test/resources/services/place/place.json").getLines().mkString("\n")
      val parseResult = Json.fromJson[Entity](Json.parse(json))
      
      parseResult.isSuccess must equalTo(true)
      
      val gazetteerRecords = parseResult.get.isConflationOf
      
      gazetteerRecords.size must equalTo(3)        
      gazetteerRecords must containAllOf(Seq(pleiadesRecord, dareRecord, trismegistosRecord))
    }
    
  }
  
  "the flex date parser" should {
    
    "parse integer- and datestring-formatted years as equal DateTimes" in {
      
      val jsonTempBoundsInt = Json.parse("{ \"from\": -30, \"to\": 640 }")      
      val jsonTempBoundsStr = Json.parse("{ \"from\": \"-30-01-01\", \"to\": \"640-01-01\" }") 

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
  
  "temp bounds de/serialization" should {
    
    "yield dates serialized in UTC" in  {
      val tempBounds = TemporalBounds(
          new DateTime(DateTimeZone.UTC).withDate(1492, 1, 1).withTime(0, 0, 0, 0),
          new DateTime(DateTimeZone.UTC).withDate(1493, 1, 1).withTime(0, 0, 0, 0))
      
      val asJson = Json.toJson(tempBounds)
      
      (asJson \ "from").as[String] must equalTo ("1492-01-01")
      (asJson \ "to").as[String] must equalTo ("1493-01-01")
    }
    
    "maintain UTC in a parse/serialize roundtrip" in {
      val json = "{\"from\":\"1492-01-01\",\"to\":\"1493-01-01\"}"
      
      val asDateTime = Json.fromJson[TemporalBounds](Json.parse(json))
      asDateTime.isSuccess must equalTo(true)
      
      val serialized = Json.toJson(asDateTime.get)
      Json.stringify(serialized) must equalTo(json)
    }
    
  }
  
}