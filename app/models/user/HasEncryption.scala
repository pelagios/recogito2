package models.user

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64
import java.security.MessageDigest
import java.util.Arrays
import controllers.HasConfig

trait HasEncryption { self: HasConfig =>

  private val CIPHER = "AES/ECB/PKCS5Padding"

  private lazy val keySpec = self.config.getOptional[String]("recogito.email.key").flatMap { key =>
    if (key.isEmpty) {
      None
    } else {
      val md = MessageDigest.getInstance("SHA-1")
      val keyDigest = md.digest(key.getBytes)
      Some(new SecretKeySpec(Arrays.copyOf(keyDigest, 16), "AES"))
    }
  }

  def encrypt(plaintext: String) = keySpec match {
    case Some(spec) => {
      val cipher = Cipher.getInstance(CIPHER)
      cipher.init(Cipher.ENCRYPT_MODE, spec)
      Base64.encodeBase64String(cipher.doFinal(plaintext.getBytes("UTF-8")))
    }

    case None => plaintext
  }

  def decrypt(encrypted: String) = keySpec match {
    case Some(spec) => {
      val cipher = Cipher.getInstance(CIPHER)
      cipher.init(Cipher.DECRYPT_MODE, spec)
      new String(cipher.doFinal(Base64.decodeBase64(encrypted)))
    }

    case None => encrypted
  }

}
