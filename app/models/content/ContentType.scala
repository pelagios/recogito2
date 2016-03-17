package models.content

import java.io.File

object ContentType extends Enumeration {
  
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
          Right(IMAGE_UPLOAD)
        
      case _ => 
        Left(UnsupportedContentType)
    }
    
  }
  
}
