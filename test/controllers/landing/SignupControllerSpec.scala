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
      val firstRedirect = route(createUserRequest).get
      status(firstRedirect) must equalTo(SEE_OTHER)
      redirectLocation(firstRedirect) must equalTo (Some("/my"))
      
      val secondRedirect = route(FakeRequest(GET, "/my").withCookies(cookies(firstRedirect).head)).get
      status(secondRedirect) must equalTo(SEE_OTHER)
      redirectLocation(secondRedirect) must equalTo (Some("/testuser"))
      
      val profilePage = route(FakeRequest(GET, "/testuser").withCookies(cookies(firstRedirect).head)).get
      status(profilePage) must equalTo(OK)
      contentAsString(profilePage) must contain ("you are logged in")
    }
    
  }
  
}