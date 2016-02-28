package controllers.myrecogito.upload.ner

import akka.actor.ActorSystem
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
import scala.concurrent.Await
import scala.concurrent.duration._
    
@RunWith(classOf[JUnitRunner])
class NERServiceIntegrationSpec extends TestKit(ActorSystem()) with ImplicitSender with SpecificationLike {
  
  "The NER service" should {
    
    // Two test documents
    val document1 = new DocumentRecord(0, "rainer", OffsetDateTime.now, "Test Doc 1", null, null, null, null, null, null)
    val document2 = new DocumentRecord(1, "rainer", OffsetDateTime.now, "Test Doc 2", null, null, null, null, null, null)
    
    // Five fileparts - parts 1-3 on document 0, 4-5 on document 1
    val parts = (1 to 5).map(n => 
      new DocumentFilepartRecord(n, { if (n < 4) 0 else 1 }, "text-for-ner-0" + n + ".txt", ContentTypes.TEXT_PLAIN.toString, "text-for-ner-0" + n + ".txt"))
      
    val parts1 = parts // .take(3)
    // val parts2 = parts.drop(3)
      
    val dir = new File("test/resources")
    
    "start NER on the 2 test documents without blocking" in { 
      Logger.info("Submitting 2 documents to NER service")
      val startTime = System.currentTimeMillis
      NERService.spawnNERProcess(document1, parts1, dir)
      // NERService.spawnNERProcess(document2, parts2, dir)
      val duration = System.currentTimeMillis - startTime
      Logger.info("NER service accepted in " + duration + " ms")
      (System.currentTimeMillis - startTime).toInt must be <(500)
      
      "report progress until completion" in {
      
        (1 to 10).foreach(_ => {
          Logger.info("[Test ] Sending progress query...")
          val result = Await.result(NERService.queryProgress(0), 10 seconds)
          Logger.info("[Test] Progress response is here: " + result.progress.headOption.map(_.progress).toString)
          Thread.sleep(1000)
        })
        
        success
      }
      
    }
      
    "progress responses should arrive in less than 500 ms" in {
      success
    }
      
    "progress must be at 100% for all fileparts after completion" in {
      success
    }
          
    "accept progress queries after completion, until the KEEPALIVE time expires" in {
      success 
    }
    
    "reject progress queries after the KEEPALIVE time has expired" in {
      success   
    }
    
    "not have any actors left alive after the KEEPALIVE time has expired" in {
      success
    }
    
  }
  
  "The database" should {
    
    "contain the annotations after NER" in {
      success
    }
    
  }
  
}