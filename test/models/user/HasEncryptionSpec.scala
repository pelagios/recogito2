package models.user

import controllers.HasConfig
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._

class TestEncryptionService(val config: Configuration) extends HasEncryption with HasConfig

class HasEncryptionSpec extends PlaySpec with GuiceOneAppPerSuite {
  
  "With a configured key, the encryption trait" should {

    "properly encrypt the test string" in {          
      val config = Configuration.from(Map("recogito.email.key" -> "My Secret Key"))
      val crypto = new TestEncryptionService(config)
      val encrypted = crypto.encrypt("rainer@pelagios.org")
      encrypted mustBe "5TVaXcMEVkY+ixcHp9XCB5r1Eht6bFIloxnxI7QvVrg="
    }
  
    "properly decrypt the test string" in {
      val config = Configuration.from(Map("recogito.email.key" -> "My Secret Key"))
      val crypto = new TestEncryptionService(config)
      val decrypted = crypto.decrypt("5TVaXcMEVkY+ixcHp9XCB5r1Eht6bFIloxnxI7QvVrg=")
      decrypted mustBe "rainer@pelagios.org"
    }
    
  }

  "With no configured key, the encryption trait" should {
    
    "just return the original plaintext on encryption" in {
      val config = Configuration.from(Map())
      val crypto = new TestEncryptionService(config)
      val encrypted = crypto.encrypt("rainer@pelagios.org")
      encrypted mustBe "rainer@pelagios.org"
    }
  
    "just return the original plaintext on decryption" in {        
      val config = Configuration.from(Map())
      val crypto = new TestEncryptionService(config)
      val decrypted = crypto.decrypt("rainer@pelagios.org")
      decrypted mustBe "rainer@pelagios.org"
    }
    
  }
  
}