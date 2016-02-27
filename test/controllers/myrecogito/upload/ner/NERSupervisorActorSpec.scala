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
import play.api.Logger
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.duration._
    
@RunWith(classOf[JUnitRunner])
class NERSupervisorActorSpec extends TestKit(ActorSystem()) with ImplicitSender with SpecificationLike {
  
  private val KEEPALIVE = 3 seconds

  "The NERSupervisorActor" should {

    val document = new DocumentRecord(0, "rainer", OffsetDateTime.now, "The Odyssey", null, null, null, null, null, null)
    val parts = (1 to 5).map(n => 
      new DocumentFilepartRecord(0, 0, "text-for-ner-0" + n + ".txt", ContentTypes.TEXT_PLAIN.toString, "text-for-ner-0" + n + ".txt"))
    val dir = new File("test/resources")
    
    val supervisor = system.actorOf(Props(classOf[NERSupervisorActor], document, parts, dir, KEEPALIVE))
    
    "run NER on the 5 test files without error" in {  
      supervisor ! NERMessages.StartNER
      // expectMsg(1 minutes, NERMessages.NERComplete)
      
      (1 to 10).foreach(_ => {
        Thread.sleep(2000)
        supervisor ! NERMessages.QueryNERProgress
      })
      
      success
    }
    
    "should not have any child actors left after completion" in {
      // TODO
      success
    }
    
    "should report a progress of 100% after completion" in {
      // TODO
      success
    }
    
    "should be stopped after KEEPALIVE interval has expired" in {
      // TODO
      success
    }
    
  }
  
  "The database" should {
    
    "contain the annotations after NER" in {
      // TODO
      success
    }
    
  }
  
}