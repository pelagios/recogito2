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
    
  val annotationAtStart  = createAnnotationAt(6, 11)    // 'ipsum'
  val annotationAtCenter = createAnnotationAt(240, 246)
  val annotationAtEnd    = createAnnotationAt(576, 590)
  
  val snippetAtStart     = new TestHasTextSnippets().extractTextSnippet(text, annotationAtStart)
  val snippetAtCenter    = new TestHasTextSnippets().extractTextSnippet(text, annotationAtCenter)
  val snippetAtEnd       = new TestHasTextSnippets().extractTextSnippet(text, annotationAtEnd)
     
  def createAnnotationAt(start: Int, end: Int) = Annotation(
      UUID.randomUUID,
      UUID.randomUUID,
      AnnotatedObject("ivzcpqoi7qr1uo", 1, ContentType.TEXT_PLAIN),
      None,
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
  
}