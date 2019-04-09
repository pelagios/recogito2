package services.document.read.results

import org.jooq.Record
import services.ContentType
import services.generated.tables.records.DocumentRecord

case class MyDocument(
  document: DocumentRecord, 
  fileCount: Int,
  contentTypes: Seq[ContentType])

object MyDocument {

  def build(record: Record) = {
    val document = record.into(classOf[DocumentRecord])
    val fileCount = record.getValue("file_count", classOf[Integer]).toInt
    val contentTypes = 
      record
        .getValue("content_types", classOf[Array[String]])
        .toSeq
        .flatMap(ContentType.withName)

    MyDocument(document, fileCount, contentTypes)
  }

}