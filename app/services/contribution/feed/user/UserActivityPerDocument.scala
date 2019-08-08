package services.contribution.feed.user

case class UserActivityPerDocument(documentId: String, count: Long, entries: Seq[UserActivityFeedEntry])