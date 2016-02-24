package models

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.Logger
import scala.io.Source
import play.api.libs.json.Json

@RunWith(classOf[JUnitRunner])
class SignupControllerSpec extends Specification {

  private val TEXT_ANNOTATION_JSON = Source.fromFile("test/resources/text-annotation.json").getLines().mkString("\n")
  
  private val IMAGE_ANNOTATION_JSON = Source.fromFile("test/resources/image-annotation.json").getLines().mkString("\n")
  
  "annotation" should {
    
    "be properly created from JSON" in {
      val result = Json.fromJson[Annotation](Json.parse(TEXT_ANNOTATION_JSON))
      result.isSuccess must equalTo(true)
      
      val textAnnotation = result.get
      textAnnotation.hasPreviousVersions must equalTo(Some(1))
      textAnnotation.contributors.size must equalTo(1)
      textAnnotation.contributors.head must equalTo("rainer")
      textAnnotation.status.value must equalTo(AnnotationStatus.VERIFIED)
    }
    
  }
  
}