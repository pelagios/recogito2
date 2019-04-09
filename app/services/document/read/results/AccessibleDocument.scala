package services.document.read.results

import org.jooq.Record
import services.ContentType
import services.generated.tables.records.{DocumentRecord, SharingPolicyRecord}

/** Wraps and accessible document with sharing policy (if any) and minimal part info **/
case class AccessibleDocument(
  document: DocumentRecord,
  sharedVia: Option[SharingPolicyRecord],
  fileCount: Int,
  contentTypes: Seq[ContentType])

object AccessibleDocument {

  def build(record: Record) = {
    val document = record.into(classOf[DocumentRecord])
    val policy = {
      val p = record.into(classOf[SharingPolicyRecord])
      Option(p.getId).map(_ => p)
    }
    val fileCount = record.getValue("file_count", classOf[Integer]).toInt
    val contentTypes = 
      record
        .getValue("content_types", classOf[Array[String]])
        .toSeq
        .flatMap(ContentType.withName)

    AccessibleDocument(document, policy, fileCount, contentTypes)
  }

}