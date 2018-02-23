package services.visit

import java.util.UUID
import services.ContentType
import services.document.DocumentAccessLevel
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import org.joda.time.{ DateTime, DateTimeZone }
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class VisitSpec extends Specification {
  
  private val DATE_TIME_PATTERN = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
  private val visitedAt = DateTime.parse("2016-11-08T07:27:00Z", DATE_TIME_PATTERN).withZone(DateTimeZone.UTC)

  "The sample Visit" should {
    
    "be properly created from JSON" in {
      val json = Source.fromFile("test/resources/services/visit/visit.json").getLines().mkString("\n")
      val result = Json.fromJson[Visit](Json.parse(json))
            
      result.isSuccess must equalTo(true) 
      
      val visit = result.get
      visit.url must equalTo("http://recogito.pelagios.org/document/fb2f3hm1ihnwgn/part/1/edit")
      visit.referer must equalTo(Some("http://recogito.pelagios.org/rainer"))
      visit.visitedAt must equalTo(visitedAt)  
      visit.responseFormat must equalTo("text/html")
      visit.accessLevel must equalTo(Some(DocumentAccessLevel.READ))
      
      val client = visit.client
      client.userAgent must equalTo("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/52.0.2743.116 Chrome/52.0.2743.116 Safari/537.36")
      client.browser must equalTo("CHROME")
      client.os must equalTo("LINUX")
      client.deviceType must equalTo("COMPUTER")
      
      val item = visit.visitedItem.get
      item.documentId must equalTo("fb2f3hm1ihnwgn")
      item.documentOwner must equalTo("rainer")
      item.filepartId must equalTo(Some(UUID.fromString("a7126845-16ac-434b-99bd-0f297e227822")))
      item.contentType must equalTo(Some(ContentType.TEXT_PLAIN))
    }
    
  }
  
  "JSON serialization/parsing roundtrip" should {
    
    "yield an equal Visit" in {
      val visit = Visit(
        "http://recogito.pelagios.org/document/fb2f3hm1ihnwgn/part/1/edit",
        Some("http://recogito.pelagios.org/rainer"),
        visitedAt,
        Client(
          "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36",
          "CHROME",
          "LINUX",
          "COMPUTER"
        ),
        "text/html",
        Some(VisitedItem(
          "fb2f3hm1ihnwgn",
          "rainer",
          Some(UUID.randomUUID),
          Some(ContentType.TEXT_PLAIN)
        )),
        Some(DocumentAccessLevel.READ)
      )
        
      // Convert to JSON
      val serialized = Json.prettyPrint(Json.toJson(visit))
      
      val parseResult = Json.fromJson[Visit](Json.parse(serialized))
      parseResult.isSuccess must equalTo(true)
      parseResult.get must equalTo(visit)
    }
    
  }
  
}