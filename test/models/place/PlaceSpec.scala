package models.place

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class PlaceSpec extends Specification {

  private val DATE_TIME_PATTERN = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
  
  "sample place" should {
    
    "be properly created from JSON" in {
      val json = Source.fromFile("test/resources/place.json").getLines().mkString("\n")
      val result = Json.fromJson[Place](Json.parse(json))
      
      success
    }
    
  }
  
}