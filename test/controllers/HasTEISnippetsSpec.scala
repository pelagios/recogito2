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
 
  "Anchor parsing" should {
    
    "return the correct values" in {
      val anchor = 
        "from=/tei/text/body/div/p[2]::64;" +
        "to=/tei/text/body/div/p[2]::89"
      
      val parsed = new TestHasTEISnippets().parseAnchor(anchor)
      
      parsed.startPath must equalTo("/TEI/text/body/div/p[2]")
      parsed.startOffset must equalTo(64)
      parsed.endPath must equalTo("/TEI/text/body/div/p[2]")
      parsed.endOffset must equalTo(89)
    }
    
  }
  
  "Snippet extraction" should {
    
    "work for snippet inside a single text node" in {
      
      val anchor =
        "from=/tei/text/body/div/p::36;" +
        "to=/tei/text/body/div/p::50"
        
      val snippet = new TestHasTEISnippets().extractTEISnippet(TEST_FILE, anchor, 16)
      snippet.text must equalTo("O muse, of that ingenious hero who travelled...")
      snippet.offset must equalTo(16)
    }
    
  }
  
  // TODO add more difficult cases
  
}
