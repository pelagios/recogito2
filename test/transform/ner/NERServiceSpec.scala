package transform.ner

import java.io.File
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import org.joox.JOOX._
import org.pelagios.recogito.sdk.ner.EntityType
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.duration._
import scala.io.Source
import org.codehaus.plexus.util.StringInputStream
import java.io.StringWriter
import java.io.BufferedWriter

@RunWith(classOf[JUnitRunner])
class NERServiceSpec extends Specification {

  def parsePlaintext() = {
    val TEST_TEXT = Source.fromFile("test/resources/transform/ner/text-for-ner-01.txt").getLines().mkString("\n")
    NERService.parseText(TEST_TEXT)
  }
  
  def enrichTEI() = {
    val TEST_TEI = 
      new File("test/resources/transform/ner/tei-for-ner.tei.xml")
    
    val writer = new StringWriter()
    NERService.enrichTEI(TEST_TEI, Some(new BufferedWriter(writer)))
    $(writer.toString)
  }

  "The NER text parse function" should {
      
    "detect 8 Named Entites in the test text" in {
      val entities = parsePlaintext()
      entities.size must equalTo (8)
    }

    "detect 3 Locations - Pylos, Sparta and Ithaca" in {
      val entities = parsePlaintext()
      val locations = entities.filter(_.entityType == EntityType.LOCATION).map(_.chars)
      locations.size must equalTo(3)
      locations must contain("Pylos")
      locations must contain("Sparta")
      locations must contain("Ithaca")
    }

    "detect 1 date" in {
      val entities = parsePlaintext()
      entities.filter(_.entityType.equals(EntityType.DATE)).size must equalTo(1)
    }

    "detect 4 persons - Ulysses (2x), Penelope and Telemachus" in {
      val entities = parsePlaintext()
      val persons = entities.filter(_.entityType == EntityType.PERSON).map(_.chars)
      persons.size must equalTo(4)
      persons must contain("Penelope")
      persons must contain("Telemachus")
      persons.filter(_.equals("Ulysses")).size must equalTo(2)
    }

    "retain correct char offsets for each entity" in {
      val TEST_TEXT = Source.fromFile("test/resources/transform/ner/text-for-ner-01.txt").getLines().mkString("\n")
      val entities = parsePlaintext()
      entities.map(e => {
        val snippetFromSourceFile = TEST_TEXT.substring(e.charOffset, e.charOffset + e.chars.size)
        snippetFromSourceFile must equalTo(e.chars)
      })
    }

  }
  
  "The NER TEI enrichment function" should {
        
    "insert 11 placeName tags" in {
      val enriched = enrichTEI()
      enriched.find("placeName").size must equalTo(11) 
    }
    
    "insert 24 persName tags" in {
      val enriched = enrichTEI()
      enriched.find("persName").size must equalTo(24)       
    }
    
  }

}
