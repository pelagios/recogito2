package services.contribution.feed.document

case class DocumentActivityByUser(username: String, count: Long, parts: Seq[DocumentActivityByPart])
