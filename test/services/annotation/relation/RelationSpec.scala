package services.annotation.relation

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import scala.io.Source
import services.annotation.{Annotation, AnnotationBody}

@RunWith(classOf[JUnitRunner])
class RelationSpec extends Specification {
  
  private val DATE_TIME_PATTERN = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
  
  import services.annotation.BackendAnnotation._
  
  "The sample annotation" should {
  
    "be properly created from JSON" in {
      val json = Source.fromFile("test/resources/services/annotation/annotation-with-relation.json").getLines().mkString("\n")
      val result = Json.fromJson[Annotation](Json.parse(json))
      
      result.isSuccess must equalTo(true) 
      
      val relations = result.get.relations
      relations.size must equalTo(1)
      relations.head.bodies.size must equalTo(1)
      
      val body = relations.head.bodies.head
      body.hasType must equalTo(AnnotationBody.TAG)
      body.lastModifiedBy must equalTo(Some("rainer"))
      body.lastModifiedAt must equalTo(DateTime.parse("2018-05-07T15:31:00Z", DATE_TIME_PATTERN).withZone(DateTimeZone.UTC))
      body.value must equalTo("flyingTo")
    }
    
  }
  
}
