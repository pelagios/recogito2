package controllers.my.upload.tiling

import java.io.File
import org.apache.commons.io.FileUtils
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.Await
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class TilingServiceSpec extends Specification {
  
  val TEST_IMAGE = new File("test/resources/Ptolemy_map_15th_century.jpg")
  
  val TMP_DIR = {
    val dir = new File("test/resources/tmp")
    if (dir.exists)
      FileUtils.deleteDirectory(dir)
    dir
  }
  
  "The Tiling function" should {
    
    "create proper Zoomify tiles from the test image" in {
      Await.result(TilingService.createZoomify(TEST_IMAGE, TMP_DIR), 10 seconds)
      
      TMP_DIR.exists must equalTo(true)
      TMP_DIR.list.size must equalTo(2)
      new File(TMP_DIR, "ImageProperties.xml").exists must equalTo(true)
      
      val tileGroup0 = new File(TMP_DIR, "TileGroup0")
      tileGroup0.exists must equalTo(true)
      
      tileGroup0.list.size must equalTo(65)
      tileGroup0.list.filter(_.endsWith(".jpg")).size must equalTo(65)
      
      FileUtils.deleteDirectory(TMP_DIR)
      
      success
    }
    
  }
  
}
