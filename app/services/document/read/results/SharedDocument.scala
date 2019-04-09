package services.document.read.results

import org.jooq.Record
import services.ContentType
import services.generated.tables.records.{DocumentRecord, SharingPolicyRecord}

/** A document 'Shared with Me' **/
case class SharedDocument(
  document: DocumentRecord,
  sharedVia: SharingPolicyRecord,
  fileCount: Int,
  contentTypes: Seq[ContentType])


object SharedDocument {

  def build(record: Record) = {
    val document = record.into(classOf[DocumentRecord])
    val policy = record.into(classOf[SharingPolicyRecord])
    val fileCount = record.getValue("file_count", classOf[Integer]).toInt
    val contentTypes = 
      record
        .getValue("content_types", classOf[Array[String]])
        .toSeq
        .flatMap(ContentType.withName)

    SharedDocument(document, policy, fileCount, contentTypes)
  }

}
