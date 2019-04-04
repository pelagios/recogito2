package services.document

import services.generated.Tables.DOCUMENT

object DocumentSortField {

  private val SORTABLE_FIELDS = 
    DOCUMENT.fields.toSeq.map(_.getName)

  /** Just make sure people don't inject any weird shit into the sort field **/
  def sanitize(name: String): Option[String] = {
    SORTABLE_FIELDS.find(_ == name)
  }

}