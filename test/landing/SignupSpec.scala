package landing
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.Logger

@RunWith(classOf[JUnitRunner])
class SignupSpec extends Specification {
  
  val testDatabase = Map(
    "db.default.driver" -> "org.sqlite.JDBC",
    "db.default.url" -> "jdbc:sqlite::memory:"
  )
  
  val createUserRequest= FakeRequest("POST","/signup")
    .withHeaders().withFormUrlEncodedBody(("username","testuser"),("email","test@mail.com"),("password","12345"))

  "signup page" should {

    /*"send 404 on a bad request" in new WithApplication(FakeApplication(additionalConfiguration = testDatabase)){
      route(FakeRequest(GET, "/signup")) must beSome.which (status(_) == NOT_FOUND)
    }*/

    "create a user in db" in new WithBrowser(app=FakeApplication(additionalConfiguration = testDatabase)){
      val response = route(createUserRequest).get
      status(response) must equalTo(SEE_OTHER)
      redirectLocation(response) must equalTo (Some("/my-recogito"))
      val c = cookies(response)
      Logger.info(c.toString())
      //contentType(home) must beSome.which(_ == "text/html")
      val redirectResponse = route(FakeRequest(GET,"/my-recogito").withCookies(c.head)).get
      Logger.info(redirectLocation(redirectResponse).toString())
      status(redirectResponse) must equalTo(OK)
      contentAsString(redirectResponse) must contain ("you are logged in")
    }
  }
}