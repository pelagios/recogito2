package models.annotation

import java.util.UUID
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import scala.io.Source
import play.api.libs.json.Json
import org.joda.time.{ DateTime, DateTimeZone }
import org.joda.time.format.DateTimeFormat

@RunWith(classOf[JUnitRunner])
class AnnotationSpec extends Specification {
  
  private val DATE_TIME_PATTERN = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
  
  "The sample text annotation" should {
    
    "be properly created from JSON" in {
      val json = Source.fromFile("test/resources/models/annotation/text-annotation.json").getLines().mkString("\n")
      val result = Json.fromJson[Annotation](Json.parse(json))
      
      // Parsed without errors?
      result.isSuccess must equalTo(true) 
      
      // Check a sample properties
      val annotation = result.get
      annotation.annotationId must equalTo(UUID.fromString("7cfa1504-26de-45ef-a590-8b60ea8a60e8"))
      annotation.versionId must equalTo(UUID.fromString("e868423f-5ea9-42ed-bb7d-5e1fac9195a0"))
      annotation.annotates.document must equalTo("98muze1cl3saib")
      annotation.hasPreviousVersions must equalTo(Some(1))
      annotation.contributors.size must equalTo(1)
      annotation.contributors.head must equalTo("rainer")
      annotation.lastModifiedAt must equalTo(DateTime.parse("2016-02-23T18:24:00Z", DATE_TIME_PATTERN).withZone(DateTimeZone.UTC))
      
      // annotation.status.value must equalTo(AnnotationStatus.VERIFIED)
      // annotation.status.setAt must equalTo(DateTime.parse("2016-02-23T18:24:00Z", DATE_TIME_PATTERN).withZone(DateTimeZone.UTC))
      
      // Bodies
      annotation.bodies.size must equalTo(3)
      annotation.bodies(0).hasType must equalTo(AnnotationBody.QUOTE)
      annotation.bodies(1).hasType must equalTo(AnnotationBody.COMMENT)
      annotation.bodies(1).lastModifiedAt must equalTo(DateTime.parse("2016-02-23T18:12:00Z", DATE_TIME_PATTERN).withZone(DateTimeZone.UTC))
      annotation.bodies(2).lastModifiedBy must equalTo(Some("rainer"))
      annotation.bodies(2).uri must equalTo(Some("https://www.wikidata.org/wiki/Q8709"))
    }
    
  }
  
  "The sample image annotation" should {
    
    "be properly created from JSON" in {
      val json = Source.fromFile("test/resources/models/annotation/image-annotation.json").getLines().mkString("\n")
      val result = Json.fromJson[Annotation](Json.parse(json))
      
      // Parsed without errors?
      result.isSuccess must equalTo(true) 
      
      // Check a sample properties (but no need to check basic stuff - already done for text annotation)
      val annotation = result.get
      annotation.bodies(0).hasType must equalTo (AnnotationBody.TRANSCRIPTION)
      
      val anchor = Json.parse(annotation.anchor)
      anchor must equalTo(Json.parse("{ \"x\": 25, \"y\": 120 }"))
    }
    
  }
  
  "JSON serialization/parsing roundtrip" should {
    
    "yield an equal annotation" in {
      // Random UUIDs for annotation and version
      val (annotationId, versionId) = (UUID.randomUUID(), UUID.randomUUID())
      
      // Current time (we force millis to 0 because serialization will drop them!) 
      val now = DateTime.now().withZone(DateTimeZone.UTC).withMillisOfSecond(0)
      
      // Two bodies, representing result of NER, without georesolution
      val bodies = 
        Seq(
          AnnotationBody(AnnotationBody.QUOTE, None, now, Some("Meidling"), None),
          AnnotationBody(AnnotationBody.PLACE, None, now, None, None))
      
      val source = 
        Annotation(
          annotationId,
          versionId,
          AnnotatedObject("98muze1cl3saib", 1),
          None,
          Seq.empty[String],
          "char-offset:25",
          None,
          now,
          bodies)
          
      // Convert to JSON
      val serialized = Json.prettyPrint(Json.toJson(source))
      
      val parseResult = Json.fromJson[Annotation](Json.parse(serialized))
      parseResult.isSuccess must equalTo(true)
      parseResult.get must equalTo(source)
    }
    
  }
  
}