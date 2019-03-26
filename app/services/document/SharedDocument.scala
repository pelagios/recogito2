package services.document

import services.ContentType
import services.generated.tables.records.{DocumentRecord, SharingPolicyRecord}

/** A document 'Shared with Me' **/
case class SharedDocument(
  document: DocumentRecord,
  sharedVia: SharingPolicyRecord,
  fileCount: Int,
  contentTypes: Seq[ContentType])