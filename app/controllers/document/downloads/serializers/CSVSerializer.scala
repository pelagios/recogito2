package controllers.document.downloads.serializers

import models.annotation.Annotation

trait CSVSerializer extends BaseSerializer {
  
  private val SEPARATOR = ";"
  private val NEWLINE   = "\n"
  private val EMPTY     = ""
  
  def toCSV(annotations: Seq[Annotation]) = {
    
    def serializeOne(a: Annotation) = {
      val firstEntity = getFirstEntityBody(a)
      a.annotationId + SEPARATOR +
      getFirstQuote(a).getOrElse(EMPTY) + SEPARATOR +
      a.anchor + SEPARATOR +
      firstEntity.map(_.hasType.toString).getOrElse(EMPTY) + SEPARATOR +
      firstEntity.flatMap(_.uri).getOrElse(EMPTY) + SEPARATOR +
      EMPTY + SEPARATOR + // TODO resolve vocab label
      EMPTY + SEPARATOR + // TODO resolve lat
      EMPTY + SEPARATOR + // TODO resolve lng
      firstEntity.flatMap(_.status.map(_.value)).getOrElse(EMPTY) + SEPARATOR
    }
    
    val header = Seq("UUID", "QUOTE", "ANCHOR", "TYPE", "URI", "VOCAB_LABEL", "LAT", "LNG", "VERIFICATION_STATUS")
    header.mkString(SEPARATOR) + NEWLINE +
    sortByCharOffset(annotations).map(serializeOne).mkString(NEWLINE)
  }
  
}