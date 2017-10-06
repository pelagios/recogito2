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
  
  val START_PATH = "/tei/text/body/div/p[2]::82"
  val END_PATH =   "/tei/text/body/div/p[2]::89"
  
  "The TEI snippet" should {
    
    "work" in {
       new TestHasTEISnippets().extractTEISnippet(TEST_FILE, START_PATH, END_PATH)
       // TODO
       success
    }
    
  }
  
  // TODO add example where snippet crosses tag bounds
  
}
