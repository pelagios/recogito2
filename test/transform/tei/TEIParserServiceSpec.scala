package transform.tei

import java.io.File
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class TEIParserServiceSpec extends Specification {
  
  val TEST_TEI = new File("test/resources/transform/tei/odyssey.tei.xml")
    
  "The NER parse function" should {
    
    val entities = Await.result(TEIParserService.extractEntities(TEST_TEI, false), 60 seconds)
    
    // TODO implement
    "just work" in {

      // TODO
      // entities.foreach { e => play.api.Logger.info(e.toString) }
      
      entities.size must equalTo(7)
    }
    
  }
  
}