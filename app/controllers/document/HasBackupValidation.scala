package controllers.document

import collection.JavaConverters._
import java.io.{ File, InputStream }
import java.math.BigInteger
import java.security.{ DigestInputStream, MessageDigest }
import java.util.zip.ZipFile
import controllers.HasConfig
import scala.concurrent.{ ExecutionContext, Future }
import scala.io.Source

object HasBackupValidation {
  
  class InvalidSignatureException extends RuntimeException
  
  class InvalidBackupException extends RuntimeException
  
  class DocumentExistsException extends RuntimeException
    
}

trait HasBackupValidation { self: HasConfig =>
  
  protected val ALGORITHM = "SHA-256"
  
  private val SECRET = self.config.getString("play.crypto.secret")
  
  private def computeHash(stream: InputStream) = {
    val md = MessageDigest.getInstance(ALGORITHM)
    val din = new DigestInputStream(stream, md)
    
    // Weird, but din is pure side-effect - consume the stream & din computes the hash  
    while (din.read() != -1) { }
    din.close()
    
    new BigInteger(1, md.digest()).toString(16)
  }
  
  def computeSignature(metadataHash: String, fileHashes: Seq[String], annotationsHash: String) = {    
    val str = SECRET + metadataHash + fileHashes.mkString + annotationsHash
    val md = MessageDigest.getInstance(ALGORITHM).digest(str.getBytes)
    new BigInteger(1, md).toString(16)
  }
  
  def validateBackup(file: File)(implicit ctx: ExecutionContext): Future[Boolean] = Future {
    scala.concurrent.blocking {      
      val zipFile = new ZipFile(file)
      val entries = zipFile.entries.asScala.toSeq.filter(!_.getName.startsWith("__MACOSX"))
      
      def hash(filename: String) = {
        val entry = entries.filter(_.getName == filename).head
        computeHash(zipFile.getInputStream(entry))
      }
      
      val expectedSignature = {
        val metadataHash = hash("metadata.json")
        val fileHashes = entries.filter(_.getName.startsWith("parts" + File.separator))
          .map(entry => hash(entry.getName))
        val annotationsHash = hash("annotations.jsonl")
        
        computeSignature(metadataHash, fileHashes, annotationsHash)
      }

      val storedSignature = {
        val signatureEntry = entries.filter(_.getName == "signature").head
        Source.fromInputStream(zipFile.getInputStream(signatureEntry), "UTF-8").getLines.mkString("\n")
      }
            
      expectedSignature == storedSignature
    }
  }
  
}