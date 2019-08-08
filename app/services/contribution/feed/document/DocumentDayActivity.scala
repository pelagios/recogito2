package services.contribution.feed.document

import org.joda.time.DateTime

case class DocumentDayActivity(timestamp: DateTime, count: Long, users: Seq[DocumentActivityByUser])