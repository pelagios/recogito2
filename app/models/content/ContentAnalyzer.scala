package models.content

import java.io.File

object ContentAnalyzer {
  
  import ContentType._
  import ContentAnalysisFailures._
  
  /** TODO analyze based on the actual file, not just the extension! **/
  def analyzeFile(file: File): Either[ContentAnalysisFailure, (ContentType.Value, Option[String])] = {
    val extension = file.getName.substring(file.getName.lastIndexOf('.') + 1).toLowerCase 
    
    extension match {
      case "txt" =>
        Right((TEXT_PLAIN, None))
        
      case "jpg" | "tif" | "png" => analyzeImage(file) match {
        case Right(metadata) =>
          Right((IMAGE_UPLOAD, Some(metadata)))
        case Left(failure) =>
          Left(failure)
      }
        
      case _ => 
        Left(UnsupportedContentType)
    }
    
  }
  
  private def analyzeImage(file: File): Either[ContentAnalysisFailure, String] = {
    Right("")
  }
  
}

object ContentAnalysisFailures {
  
  sealed trait ContentAnalysisFailure
  
  case object UnsupportedContentType extends ContentAnalysisFailure
  
  case object CorruptImage extends ContentAnalysisFailure
  
}