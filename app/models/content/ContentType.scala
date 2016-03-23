package models.content

import java.io.File
import play.api.Logger
import scala.language.postfixOps
import scala.util.Try
import sys.process._

object ContentType extends Enumeration {

  // Images are only supported if VIPS is installed on the system
  private val VIPS_INSTALLED = {
    val testVips = Try("vips help" !)
    if (testVips.isFailure)
      Logger.warn("VIPS not installed - image support disabled")
      
    testVips.isSuccess
  }

  import ContentIdentificationFailures._

  val TEXT_PLAIN =    Value("TEXT_PLAIN")

  val TEXT_MARKDOWN = Value("TEXT_MARKDOWN")

  val TEXT_TEIXML =   Value("TEXT_TEIXML")

  val IMAGE_UPLOAD =  Value("IMAGE_UPLOAD")

  val IMAGE_IIIF =    Value("IMAGE_IIIF")

  val DATA_CSV =      Value("DATA_CSV")

  /** TODO analyze based on the actual file, not just the extension! **/
  def fromFile(file: File): Either[ContentIdentificationFailure, ContentType.Value] = {
    val extension = file.getName.substring(file.getName.lastIndexOf('.') + 1).toLowerCase

    extension match {
      case "txt" =>
        Right(TEXT_PLAIN)

      case "jpg" | "tif" | "png" =>
        if (VIPS_INSTALLED) Right(IMAGE_UPLOAD) else Left(UnsupportedContentType)

      case _ =>
        Left(UnsupportedContentType)
    }

  }

}
