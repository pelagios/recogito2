package controllers.landing

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.Logger

@RunWith(classOf[JUnitRunner])
class SignupControllerSpec extends Specification {
  
  // In-memory SQLite
  val testDatabase = Map(
    "db.default.driver" -> "org.sqlite.JDBC",
    "db.default.url" -> "jdbc:sqlite::memory:"
  )
  
  // Signup form submit
  val createUserRequest = 
    FakeRequest("POST","/signup")
      .withHeaders()
      .withFormUrlEncodedBody(("username", "testuser"), ("email", "test@mail.com"), ("password", "12345"))

  "The signup page" should {

    "create a user in the DB" in new WithBrowser(app = FakeApplication(additionalConfiguration = testDatabase)) {
      val redirect = route(createUserRequest).get
      
      status(redirect) must equalTo(SEE_OTHER)
      redirectLocation(redirect) must equalTo (Some("/my-recogito"))
      
      val myRecogito = route(FakeRequest(GET,"/my-recogito").withCookies(cookies(redirect).head)).get
      status(myRecogito) must equalTo(OK)
      contentAsString(myRecogito) must contain ("you are logged in")
    }
    
  }
  
}