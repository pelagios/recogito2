package controllers

import java.util.UUID
import models.ContentType
import models.annotation._
import org.joda.time.DateTime
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._

class TestHasTextSnippets extends HasTextSnippets

@RunWith(classOf[JUnitRunner])
class HasTextSnippetsSpec extends Specification {

  val text = 
    "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam " + 
    "nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, " +
    "sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. " +
    "Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor " +
    "sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam " +
    "nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam " +
    "voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita " +
    "kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet."
    
  val annotationAtStart  = createAnnotationAt(6, 11, text)
  val annotationAtCenter = createAnnotationAt(240, 246, text)
  val annotationAtEnd    = createAnnotationAt(576, 590, text)
  
  val snippetAtStart     = new TestHasTextSnippets().extractTextSnippet(text, annotationAtStart)
  val snippetAtCenter    = new TestHasTextSnippets().extractTextSnippet(text, annotationAtCenter)
  val snippetAtEnd       = new TestHasTextSnippets().extractTextSnippet(text, annotationAtEnd)
  
  val evilText = // Snippets determine bounds on whitespace - what if there is none?
    "Loremipsumdolorsitametconsetetursadipscingelitrseddiam" + 
    "nonumyeirmodtemporinviduntutlaboreetdoloremagnaaliquyamerat"
    
  val annotationOnEvilText = createAnnotationAt(18, 28, evilText)
  
  val snippetOnEvilText = new TestHasTextSnippets().extractTextSnippet(evilText, annotationOnEvilText)
    
  def createAnnotationAt(start: Int, end: Int, text: String) = Annotation(
      UUID.randomUUID,
      UUID.randomUUID,
      AnnotatedObject("ivzcpqoi7qr1uo", UUID.randomUUID, ContentType.TEXT_PLAIN),
      Seq("rainer"),
      "char-offset:" + start,
      Some("rainer"),
      DateTime.now,
      Seq(AnnotationBody(
        AnnotationBody.QUOTE,
        Some("rainer"),
        DateTime.now,
        Some(text.substring(start, end)),
        None,
        None)))
        
  "The snippet for the first annotation" should {
    
    "start with 'Lorem'" in {
      snippetAtStart.text.startsWith("Lorem") must equalTo(true)
    }
    
    "end with 'tempor...'" in {
      snippetAtStart.text.endsWith("tempor...") must equalTo(true)
    }
    
    "have an offset of 6" in {
      snippetAtStart.offset must equalTo(6)
    }
    
  }

  "The snippet for the second annotation" should {
    
    "start with '...eos et'" in {
      snippetAtCenter.text.startsWith("...eos et") must equalTo(true)
    }
    
    "end with 'dolor sit amet...'" in {
      snippetAtCenter.text.endsWith("dolor sit amet...") must equalTo(true)
    }
    
    "have an offset of 78" in {
      snippetAtCenter.offset must equalTo(78)
    }
    
  }
  
  "The snippet for the third annotation" should {
    
    "start with 'Loremipsumdolor'" in {
      snippetAtEnd.text.startsWith("...ea rebum") must equalTo(true)
    }
    
    "end with 'dolor sit amet.'" in {
      snippetAtEnd.text.endsWith("dolor sit amet.") must equalTo(true)
    }
    
    "have an offset of 79" in {
      snippetAtEnd.offset must equalTo(79)
    }
    
  }

  "The snippet for the space-less text" should {
    
    play.api.Logger.info(snippetOnEvilText.toString)
    
    "start with 'Loremipsum'" in {
      snippetOnEvilText.text.startsWith("Loremipsum") must equalTo(true)
    }
    
    "end with 'magnaaliquya...'" in {
      snippetOnEvilText.text.endsWith("magnaaliquya...") must equalTo(true)
    }
    
    "have an offset of 18" in {
      snippetOnEvilText.offset must equalTo(18)
    }
    
  }

  
}