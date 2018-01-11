package transform.tei

import java.io.File
import java.util.UUID
import services.ContentType
import services.generated.tables.records.DocumentFilepartRecord
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.io.Source
import services.annotation.AnnotationBody

@RunWith(classOf[JUnitRunner])
class TEIParserServiceSpec extends Specification {
  
  val application = GuiceApplicationBuilder().build()
  implicit val executionContext = application.injector.instanceOf[ExecutionContext]

  val TEST_FILEPART_RECORD = new DocumentFilepartRecord(
    UUID.randomUUID,
    "hcylkmacy4xgkb", 
    "The Odyssey", ContentType.TEXT_TEIXML.toString, "odyssey.tei.xml", 0, null)

  val TEST_FILE = new File("test/resources/transform/tei/odyssey.tei.xml")
        
  val EXPECTED_ENTITIES = Seq(
    // Places
    ("from=/tei/text/body/div/p::127;to=/tei/text/body/div/p::131",       "Troy"),
    ("from=/tei/text/body/div/p[2]::401;to=/tei/text/body/div/p[2]::407", "Ithaca"),
    ("from=/tei/text/body/div/p[5]::987;to=/tei/text/body/div/p[5]::991", "Troy"),
    ("from=/tei/text/body/div/p[7]::182;to=/tei/text/body/div/p[7]::196", "Ogygian island"),
    ("from=/tei/text/body/div/p[7]::329;to=/tei/text/body/div/p[7]::335", "Ithaca"),
    ("from=/tei/text/body/div/p[7]::613;to=/tei/text/body/div/p[7]::619", "Sparta"),
    ("from=/tei/text/body/div/p[7]::637;to=/tei/text/body/div/p[7]::642", "Pylos"),
    // People
    ("from=/tei/text/body/div/p::515;to=/tei/text/body/div/p::523",       "Hyperion"),
    ("from=/tei/text/body/div/p[2]::103;to=/tei/text/body/div/p[2]::110", "Ulysses"),
    ("from=/tei/text/body/div/p[2]::215;to=/tei/text/body/div/p[2]::222", "Calypso"),
    ("from=/tei/text/body/div/p[6]::92;to=/tei/text/body/div/p[6]::99",   "Ulysses"),
    // Spans
    ("from=/tei/text/body/div/p[5]::229;to=/tei/text/body/div/p[5]::244", "my heart bleeds"))
    
  "The TEI parser" should {
    
    val annotations = Await.result(TEIParserService.extractEntities(TEST_FILEPART_RECORD, TEST_FILE, false), 60 seconds)
    
    "properly parse the test document" in {      
      annotations.size must equalTo(12)
      
      val places = annotations.filter(_.bodies.exists(_.hasType == AnnotationBody.PLACE))
      places.size must equalTo(7)
      
      val people = annotations.filter(_.bodies.exists(_.hasType == AnnotationBody.PERSON))
      people.size must equalTo(4)
      
      annotations.map { a =>
        val anchor = a.anchor
        val quote = a.bodies.find(_.hasType == AnnotationBody.QUOTE).get.value.get
        EXPECTED_ENTITIES must contain((anchor, quote))
      }
    }
    
  }
  
}