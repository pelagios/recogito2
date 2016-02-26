package controllers.myrecogito.upload.ner

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import java.time.OffsetDateTime
import models.ContentTypes
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.Logger
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class NERServiceSpec extends Specification {

  val TEST_TEXT = 
    Source.fromFile("test/resources/text-for-ner.txt").getLines().mkString("\n")
  
  "The NER parse function" should {
    
    val entities = NERService.parse(TEST_TEXT)
    
    "detect 11 Named Entites in the test text" in {
      entities.size must equalTo (11)
    }
    
    "detect 3 Locations - Pylos, Sparta and Ithaca" in {
      val locations = entities.filter(_.entityTag == "LOCATION").map(_.chars)
      locations.size must equalTo(3)
      locations must contain("Pylos")
      locations must contain("Sparta")
      locations must contain("Ithaca")
    }
    
    "detect 1 date" in {
      entities.filter(_.entityTag.equals("DATE")).size must equalTo(1)
    }
    
    "detect 4 persons - Ulysses (2x), Penelope and Telemachus" in {
      val persons = entities.filter(_.entityTag == "PERSON").map(_.chars)
      persons.size must equalTo(4)
      persons must contain("Penelope")
      persons must contain("Telemachus")
      persons.filter(_.equals("Ulysses")).size must equalTo(2)
    }
    
    "retain correct char offsets for each entity" in {
      entities.map(e => {
        val snippetFromSourceFile = TEST_TEXT.substring(e.charOffset, e.charOffset + e.chars.size)
        snippetFromSourceFile must equalTo(e.chars)
      })
    }
    
  }
  
  "The NER service" should {

    implicit val actorSystem = ActorSystem("testActorSystem", ConfigFactory.load())
    
    // TODO need to find a way to make the NER actors more testable (with absolute filepaths!)
    
    // TODO revisit the filepart DB schema (title vs. name vs. path etc.)
    
    val document = new DocumentRecord(0, "rainer", OffsetDateTime.now, "The Odyssey", null, null, null, null, null, null)
    val filepart = new DocumentFilepartRecord(0, 0, "text-for-ner.txt", ContentTypes.TEXT_PLAIN.toString, "test/resources/text-for-ner.txt")
    
    "spawn 100 actors without ID collisions" in {
      (1 to 100).map(_ => {
        val actorId = NERService.spawnParseProcess(document, Seq(filepart))
        Logger.info(actorId)
        1 must equalTo(1)
      })
    }
    
  }
  
}