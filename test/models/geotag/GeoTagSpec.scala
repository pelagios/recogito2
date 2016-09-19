package models.geotag

import java.util.UUID
import org.joda.time.{ DateTime, DateTimeZone }
import org.joda.time.format.DateTimeFormat
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class GeoTagSpec extends Specification {
  
  private val DATE_TIME_PATTERN = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")

  "The sample geotag" should {
    
    "be properly created from JSON" in {
      val json = Source.fromFile("test/resources/models/geotag/geotag.json").getLines().mkString("\n")
      val result = Json.fromJson[GeoTag](Json.parse(json))
      
      // Parsed without errors?
      result.isSuccess must equalTo(true) 
      
      val geotag = result.get
      geotag.annotationId must equalTo(UUID.fromString("5c25d207-11a5-49f0-b2a7-61a6ae63d96c"))
      geotag.documentId must equalTo("qhljvnxnuuc9i0")
      geotag.filepartId must equalTo(UUID.fromString("f903b736-cae8-4fe3-9bda-01583783548b"))
      geotag.gazetteerUri must equalTo("http://pleiades.stoa.org/places/118543")
      geotag.lastModifiedAt must equalTo(DateTime.parse("2016-09-19T13:09:00Z", DATE_TIME_PATTERN).withZone(DateTimeZone.UTC))
    }
    
  }
  
  "JSON serialization/parsing roundtrip" should {
    
    "yield an equal geotag" in {
      val geotag = GeoTag(
        UUID.randomUUID(),
        "qhljvnxnuuc9i0",
        UUID.fromString("841f9462-beb0-4967-ad48-64af323fc4c1"),
        "http://pleiades.stoa.org/places/118543",
        Seq.empty[String], // toponym
        Seq.empty[String], // contributors
        None, // lastModifiedBy
        DateTime.parse("2016-02-23T18:24:00Z", DATE_TIME_PATTERN).withZone(DateTimeZone.UTC))
        
      // Convert to JSON
      val serialized = Json.prettyPrint(Json.toJson(geotag))
      
      val parseResult = Json.fromJson[GeoTag](Json.parse(serialized))
      parseResult.isSuccess must equalTo(true)
      parseResult.get must equalTo(geotag)
    }
    
  }
  
}