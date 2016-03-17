package models.content

import java.io.File
import models.content.ContentIdentificationFailures._

object ContentType extends Enumeration {

  val TEXT_PLAIN =    Value("TEXT_PLAIN")

  val TEXT_MARKDOWN = Value("TEXT_MARKDOWN")

  val TEXT_TEIXML =   Value("TEXT_TEIXML")

  val IMAGE_UPLOAD =  Value("IMAGE_UPLOAD")

  val IMAGE_IIIF =    Value("IMAGE_IIIF")

  val DATA_CSV =      Value("DATA_CSV")
  
  /** Identifies the content type of a file 
    *
    * TODO identify from the actual file, not just from the extension
    */
  def fromFile(file: File): Either[ContentIdentificationFailure, ContentType.Value] = {
    val extension = file.getName.substring(file.getName.lastIndexOf('.') + 1).toLowerCase 
    extension match {
      case "txt" =>
        Right(TEXT_PLAIN)
      case "jpg" | "tif" | "png" => 
        Right(IMAGE_UPLOAD)
      case _ => 
        Left(UnsupportedContentType)
    }
  }
  
}
