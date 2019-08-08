package services.contribution.feed.document

import java.util.UUID

case class DocumentActivityByPart(partId: UUID, count: Long, entries: Seq[DocumentActivityFeedEntry])