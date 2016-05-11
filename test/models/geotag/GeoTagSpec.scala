package models.geotag

import java.util.UUID
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class GeoTagSpec extends Specification {

  "The sample geotag" should {
    
    "be properly created from JSON" in {
      val json = Source.fromFile("test/resources/models/geotag/geotag.json").getLines().mkString("\n")
      val result = Json.fromJson[GeoTag](Json.parse(json))
      
      // Parsed without errors?
      result.isSuccess must equalTo(true) 
      
      val geotag = result.get
      geotag.placeId must equalTo("http://dare.ht.lu.se/places/10778")
      geotag.annotationId must equalTo(UUID.fromString("5c25d207-11a5-49f0-b2a7-61a6ae63d96c"))
      geotag.documentId must equalTo("qhljvnxnuuc9i0")
      geotag.filepartId must equalTo(19)
      geotag.gazetteerUri must equalTo("http://pleiades.stoa.org/places/118543")
    }
    
  }
  
  "JSON serialization/parsing roundtrip" should {
    
    "yield an equal geotag" in {
      val geotag = GeoTag(
        "http://dare.ht.lu.se/places/10778",
        UUID.randomUUID(),
        "qhljvnxnuuc9i0",
        19,
        "http://pleiades.stoa.org/places/118543")
        
      // Convert to JSON
      val serialized = Json.prettyPrint(Json.toJson(geotag))
      
      val parseResult = Json.fromJson[GeoTag](Json.parse(serialized))
      parseResult.isSuccess must equalTo(true)
      parseResult.get must equalTo(geotag)
    }
    
  }
  
}