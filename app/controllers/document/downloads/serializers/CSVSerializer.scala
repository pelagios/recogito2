package controllers.document.downloads.serializers

import models.annotation.Annotation

trait CSVSerializer extends BaseSerializer {
  
  private val SEPARATOR = ";"
  private val NEWLINE   = "\n"
  private val EMPTY     = ""
  
  def toCSV(annotations: Seq[Annotation]) = {
    
    def serializeOne(a: Annotation) = {
      getFirstQuote(a).getOrElse(EMPTY) + SEPARATOR + 
      EMPTY + SEPARATOR + // TODO type
      EMPTY + SEPARATOR + // TODO uri
      EMPTY + SEPARATOR + // TODO vocab label
      EMPTY + SEPARATOR + // TODO lat
      EMPTY + SEPARATOR + // TODO lng
      EMPTY + SEPARATOR // TODO status
    }
    
    val header = Seq("quote", "type", "uri", "vocab_label", "lat", "lng", "verification_status")
    header.mkString(SEPARATOR) + NEWLINE +
    annotations.map(serializeOne(_)).mkString(NEWLINE)
  }
  
}