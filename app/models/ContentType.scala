package models

import java.io.File
import play.api.Logger
import scala.util.Try
import scala.io.Source
import scala.language.postfixOps
import sys.process._

sealed trait ContentType {
  
  val media: String
  
  val subtype: String
  
  lazy val name = media + "_" + subtype

  lazy val isImage = media == "IMAGE"

  lazy val isText  = media == "TEXT"

  lazy val isData  = media == "DATA"
  
  lazy val isLocal =
    // No other remote types supported at present
    subtype != "IIIF" 
  
}

class UnsupportedContentTypeException extends RuntimeException

class UnsupportedTextEncodingException extends RuntimeException

object ContentType {
  
  case object TEXT_PLAIN    extends ContentType { val media = "TEXT"  ; val subtype = "PLAIN" }
  case object TEXT_TEIXML   extends ContentType { val media = "TEXT"  ; val subtype = "TEIXML" }
  case object TEXT_MARKDOWN extends ContentType { val media = "TEXT"  ; val subtype = "MARKDOWN" }

  case object IMAGE_UPLOAD  extends ContentType { val media = "IMAGE" ; val subtype = "UPLOAD" }
  case object IMAGE_IIIF    extends ContentType { val media = "IMAGE" ; val subtype = "IIIF" }

  case object DATA_CSV      extends ContentType { val media = "DATA"  ; val subtype = "CSV" }

  def withName(name: String): Option[ContentType] = Seq(
    TEXT_PLAIN,
    TEXT_TEIXML,
    TEXT_MARKDOWN,
    IMAGE_UPLOAD,
    IMAGE_IIIF,
    DATA_CSV).find(_.name == name)
  
  // Images are only supported if VIPS is installed on the system
  private val VIPS_INSTALLED = {
    val testVips = Try("vips help" !)
    if (testVips.isFailure)
      Logger.warn("VIPS not installed - image support disabled")
      
    testVips.isSuccess
  }

  /** TODO analyze based on the actual file, not just the extension! **/
  def fromFile(file: File): Either[Exception, ContentType] = {
    
    def getIfReadableTextFile(file: File, cType: ContentType): Either[Exception, ContentType] =
      try {
        Source.fromFile(file).getLines.mkString("\n")
        Right(cType)
      } catch { 
        case t: java.nio.charset.MalformedInputException => Left(new UnsupportedTextEncodingException)
        case t: Throwable => throw t
      }
    
    val extension = file.getName.substring(file.getName.lastIndexOf('.') + 1).toLowerCase
    extension match {
      
      case "txt" => getIfReadableTextFile(file, TEXT_PLAIN)
        
      case "xml" => getIfReadableTextFile(file, TEXT_TEIXML)
        
      case "csv" => getIfReadableTextFile(file, DATA_CSV)

      case "jpg" | "jpeg" | "tif" | "tiff" | "png" =>
        if (VIPS_INSTALLED) Right(IMAGE_UPLOAD) else Left(new UnsupportedContentTypeException)
        
      case _ => Left(new UnsupportedContentTypeException)
      
    }

  }
  
}