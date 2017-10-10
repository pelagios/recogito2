package controllers;

import java.io.File
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._

class TestHasTEISnippets extends HasTEISnippets

@RunWith(classOf[JUnitRunner])
class HasTEISnippetsSpec extends Specification {
  
  val TEST_FILE = new File("test/resources/transform/tei/odyssey.tei.xml")
  
  val ANCHOR = 
    "from=/tei/text/body/div/p[2]::82;" +
    "to=/tei/text/body/div/p[2]::89"
    
  "Anchor parsing" should {
    
    "return the correct values" in {
      val parsed = new TestHasTEISnippets().parseAnchor(ANCHOR)
      parsed.startPath must equalTo("/TEI/text/body/div/p[2]")
      parsed.startOffset must equalTo(82)
      parsed.endPath must equalTo("/TEI/text/body/div/p[2]")
      parsed.endOffset must equalTo(89)
    }
    
  }
  
  "Snippet extraction" should {
    
    "return the correct snippet" in {
       new TestHasTEISnippets().extractTEISnippet(TEST_FILE, ANCHOR)
       
       
       // TODO
       success
    }
    
  }
  
  // TODO add example where snippet crosses tag bounds
  
}
