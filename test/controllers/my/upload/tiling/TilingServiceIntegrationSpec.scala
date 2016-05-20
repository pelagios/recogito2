package controllers.my.upload.tiling

import akka.actor.ActorSystem
import akka.testkit.{ TestKit, ImplicitSender }
import controllers.my.upload.ProgressStatus
import java.io.File
import java.sql.Timestamp
import models.ContentType
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.apache.commons.io.FileUtils
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.AfterAll
import org.junit.runner._
import play.api.Logger
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Await
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class TilingServiceIntegrationSpec extends TestKit(ActorSystem()) with ImplicitSender with SpecificationLike with AfterAll {
  
  // Force Specs2 to execute tests in sequential order
  sequential 
  
  private val DEST_DIR = new File("test/resources/controllers/my/upload/tiling/Ptolemy_map_15th_century")
  
  override def afterAll = FileUtils.deleteDirectory(DEST_DIR)
  
  "The Tiling service" should {

    FileUtils.deleteDirectory(DEST_DIR)
    
    val KEEP_ALIVE = 10 seconds
    
    val document = new DocumentRecord("hcylkmacy4xgkb", "rainer", new Timestamp(System.currentTimeMillis), "A test image", null, null, null, null, null, null)
    val parts = Seq(new DocumentFilepartRecord(1, "hcylkmacy4xgkb", "Ptolemy_map_15th_century.jpg", ContentType.IMAGE_UPLOAD.toString, "Ptolemy_map_15th_century.jpg", 0))
    val dir = new File("test/resources/controllers/my/upload/tiling")
    
    
    val processStartTime = System.currentTimeMillis
    TilingService.spawnTask(document, parts, dir, KEEP_ALIVE)
    
    "start tiling on the test image without blocking" in { 
      (System.currentTimeMillis - processStartTime).toInt must be <(1000)
    }
    
    "report progress until complete" in {
      var ctr = 50
      var isComplete = false
      
      while (ctr > 0 && !isComplete) {
        ctr -= 1
        
        val queryStartTime = System.currentTimeMillis
        
        val result = Await.result(TilingService.queryProgress(document.getId), 10 seconds)
        
        result.isDefined must equalTo(true)
        result.get.progress.size must equalTo(1)
        
        val progress = result.get.progress.head  
        Logger.info("[TilingServiceIntegrationSpec] Image tiling progress is " + progress.status + " - " + progress.progress)
        
        if (progress.progress == 1.0)
          isComplete = true
        
        Thread.sleep(2000)
      }
      
      success
    }
    
    "have created the image tileset" in {
      DEST_DIR.exists must equalTo(true)
      DEST_DIR.list.size must equalTo(2)
      new File(DEST_DIR, "ImageProperties.xml").exists must equalTo(true)
      
      val tileGroup0 = new File(DEST_DIR, "TileGroup0")
      tileGroup0.exists must equalTo(true)
      
      tileGroup0.list.size must equalTo(65)
      tileGroup0.list.filter(_.endsWith(".jpg")).size must equalTo(65)
    }
    
    "accept progress queries after completion" in {
      val queryStartTime = System.currentTimeMillis
      
      val result = Await.result(TilingService.queryProgress(document.getId), 10 seconds)
      
      (System.currentTimeMillis - queryStartTime).toInt must be <(500)
      
      result.isDefined must equalTo(true) 
      result.get.progress.size must equalTo(1)
      result.get.progress.head.progress must equalTo(1.0)
      result.get.progress.head.status must equalTo(ProgressStatus.COMPLETED)      
    }
    
    "reject progress queries after the KEEPALIVE time has expired" in {
      Thread.sleep(KEEP_ALIVE.toMillis)
      Logger.info("[TilingServiceIntegrationSpec] KEEPALIVE expired")
      
      val result = Await.result(TilingService.queryProgress(document.getId), 10 seconds)
      
      result.isDefined must equalTo(false)
    }
    
  }
  
}