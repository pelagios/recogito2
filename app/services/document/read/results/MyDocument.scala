package services.document.read.results

import org.jooq.Record
import services.ContentType
import services.generated.tables.records.DocumentRecord

case class MyDocument(
  document: DocumentRecord, 
  fileCount: Int,
  contentTypes: Seq[ContentType],
  clonedFromUser: Option[String],
  hasClones: Int)

object MyDocument {

  def build(record: Record) = {
    val document = record.into(classOf[DocumentRecord])
    val fileCount = record.getValue("file_count", classOf[Integer]).toInt
    val clonedFromUser = Option(record.getValue("cloned_from_user", classOf[String]))
    val hasClones = Option(record.getValue("has_clones", classOf[Integer])).map(_.toInt).getOrElse(0)
    val contentTypes = 
      record
        .getValue("content_types", classOf[Array[String]])
        .toSeq
        .flatMap(ContentType.withName)

    MyDocument(document, fileCount, contentTypes, clonedFromUser, hasClones)
  }

}