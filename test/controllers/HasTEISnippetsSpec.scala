package controllers;

import java.io.File
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import org.joox.JOOX._
import play.api.test._
import play.api.test.Helpers._
import scala.io.Source

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
  
  "Node flattening" should {
    
    "work properly in the test structure" in {
      val doc = $("""
        <a>
          <b>
            <c></c>
            <d></d>
          </b>
          <e>
            <f></f>
            <g>
              <h></h>
              <i></i>
            </g>
          </e>
        </a>  
      """
      // Remove all whitespace - we don't want text nodes in between
      .replaceAll("\\s+", "").trim).document().getFirstChild
      
      val flattened = new TestHasTEISnippets().flattenDOM(doc).map(_.getNodeName)
      flattened must equalTo(List("a", "b", "c", "d", "e", "f", "g", "h", "i"))
    }
    
  }
  
  "Preview generation" should {
    
    val xml = Source.fromFile(TEST_FILE).getLines().mkString("\n")
    val parser = new TestHasTEISnippets()
    
    "render the correct short preview" in {  
      val shortPreview = parser.previewFromTEI(xml, 15)
      shortPreview must equalTo("Tell me, O muse")
    }
    
    "render the correct long preview" in {
      val longPreview = parser.previewFromTEI(xml, 111)
      longPreview must equalTo("Tell me, O muse, of that ingenious hero who travelled far and wide after he had sacked the famous town of Troy.") 
    }
    
  }
  
  "Snippet extraction" should {

    "work for snippet inside a single text node" in {
      val anchor =
        "from=/tei/text/body/div/p::35;" +
        "to=/tei/text/body/div/p::50"
        
      val snippet = new TestHasTEISnippets().snippetFromTEIFile(TEST_FILE, anchor, 16)
      snippet.text must equalTo("...O muse, of that ingenious hero who travelled...")
      snippet.offset must equalTo(17)
    }
    
    "work across entity boundaries" in {      
      val anchorA =
        "from=/tei/text/body/div/p::84;" +
        "to=/tei/text/body/div/p::170"
        
      val anchorB =
        "from=/tei/text/body/div/p::126;" +
        "to=/tei/text/body/div/p::170"
        
      val anchorC =
        "from=/tei/text/body/div/p::84;" +
        "to=/tei/text/body/div/p::133"
        
      val snippetA = new TestHasTEISnippets().snippetFromTEIFile(TEST_FILE, anchorA, 0)
      snippetA.text must equalTo("...had sacked the famous town of Troy. Many cities did he visit...")
      
      val snippetB = new TestHasTEISnippets().snippetFromTEIFile(TEST_FILE, anchorB, 0)
      snippetB.text must equalTo("...Troy. Many cities did he visit...")
      
      val snippetC = new TestHasTEISnippets().snippetFromTEIFile(TEST_FILE, anchorC, 0)
      snippetC.text must equalTo("...had sacked the famous town of Troy...")
    }
    
  }
    
}
