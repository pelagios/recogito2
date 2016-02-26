package controllers.myrecogito.upload.ner

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ TestKit, ImplicitSender }
import java.io.File
import java.time.OffsetDateTime
import models.ContentTypes
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class NERActorSpec extends TestKit(ActorSystem()) with ImplicitSender with SpecificationLike {

  "The NER actor" should {

    val document = new DocumentRecord(0, "rainer", OffsetDateTime.now, "The Odyssey", null, null, null, null, null, null)
    val filepart = new DocumentFilepartRecord(0, 0, "text-for-ner.txt", ContentTypes.TEXT_PLAIN.toString, "text-for-ner.txt")
    val file = new File("test/resources/text-for-ner.txt")
    
    "just work" in {
      val actor = system.actorOf(Props(classOf[NERActor], document, Seq(filepart, file)))
      actor ! NERActor.StartNER
      expectMsg(NERActor.NERComplete)

      success
    }
    
  }
  
}