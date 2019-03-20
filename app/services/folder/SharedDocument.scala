package services.folder

import services.ContentType
import services.generated.tables.records.{DocumentRecord, SharingPolicyRecord}

case class SharedDocument(
  document: DocumentRecord,
  sharedVia: SharingPolicyRecord,
  fileCount: Int,
  contentTypes: Seq[ContentType])