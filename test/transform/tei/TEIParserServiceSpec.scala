package transform.tei

import java.io.File
import java.util.UUID
import models.ContentType
import models.generated.tables.records.DocumentFilepartRecord
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source
import models.annotation.AnnotationBody

@RunWith(classOf[JUnitRunner])
class TEIParserServiceSpec extends Specification {

  val TEST_FILEPART_RECORD = new DocumentFilepartRecord(
    UUID.randomUUID,
    "hcylkmacy4xgkb", 
    "The Odyssey", ContentType.TEXT_TEIXML.toString, "odyssey.tei.xml", 0, null)

  val TEST_FILE = new File("test/resources/transform/tei/odyssey.tei.xml")
        
  val EXPECTED_ENTITIES = Seq(
    ("from=/tei/text/body/div/p::107;to=/tei/text/body/div/p::111",   "Troy"),
    ("from=/tei/text/body/div/p::947;to=/tei/text/body/div/p::953",   "Ithaca"),
    ("from=/tei/text/body/div/p::3016;to=/tei/text/body/div/p::3020", "Troy"),
    ("from=/tei/text/body/div/p::3976;to=/tei/text/body/div/p::3990", "Ogygian Island"),
    ("from=/tei/text/body/div/p::4093;to=/tei/text/body/div/p::4099", "Ithaca"),
    ("from=/tei/text/body/div/p::4337;to=/tei/text/body/div/p::4343", "Sparta"),
    ("from=/tei/text/body/div/p::4351;to=/tei/text/body/div/p::4356", "Pylos"))

  "The TEI parser" should {
    
    val annotations = Await.result(TEIParserService.extractEntities(TEST_FILEPART_RECORD, TEST_FILE, false), 60 seconds)
    
    "properly parse the test document" in {      
      annotations.size must equalTo(EXPECTED_ENTITIES.size)
      annotations.map { a =>
        val anchor = a.anchor
        val quote = a.bodies.find(_.hasType == AnnotationBody.QUOTE).get.value.get
        EXPECTED_ENTITIES must contain((anchor, quote))
      }
    }
    
  }
  
}