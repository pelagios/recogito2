package models

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.Logger

@RunWith(classOf[JUnitRunner])
class SignupControllerSpec extends Specification {
  
  "annotation" should {
    
    "be properly created from JSON" in {
      
      1 must equalTo(1)
    }
    
  }
  
}