package models.user

import controllers.HasConfig
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.{ Configuration, Play }
import play.api.test._
import play.api.test.Helpers._

class TestEncryptionService(val config: Configuration) extends HasEncryption with HasConfig

@RunWith(classOf[JUnitRunner])
class HasEncryptionSpec extends Specification {
  
  running (FakeApplication(additionalConfiguration = Map("recogito.email.key" -> "My Secret Key"))) {
    
    val crypto = new TestEncryptionService(Play.current.configuration)
   
    "With a configured key, the encryption trait" should {

      "properly encrypt the test string" in {
        val encrypted = crypto.encrypt("rainer@pelagios.org")
        encrypted must equalTo("5TVaXcMEVkY+ixcHp9XCB5r1Eht6bFIloxnxI7QvVrg=")
      }
    
      "properly decrypt the test string" in {
        val decrypted = crypto.decrypt("5TVaXcMEVkY+ixcHp9XCB5r1Eht6bFIloxnxI7QvVrg=")
        decrypted must equalTo("rainer@pelagios.org")
      }
      
    }
    
  }
  
  running (FakeApplication(additionalConfiguration = Map("recogito.email.key" -> ""))) { 
    
    val crypto = new TestEncryptionService(Play.current.configuration)
   
    "With no configured key, the encryption trait" should {

      "just return the original plaintext on encryption" in {
        val encrypted = crypto.encrypt("rainer@pelagios.org")
        encrypted must equalTo("rainer@pelagios.org")
      }
    
      "just return the original plaintext on decryption" in {
        val decrypted = crypto.decrypt("rainer@pelagios.org")
        decrypted must equalTo("rainer@pelagios.org")
      }
      
    }
    
  }
  

  
}