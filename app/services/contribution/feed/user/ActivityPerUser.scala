package services.contribution.feed.user

case class ActivityPerUser(username: String, count: Long, documents: Seq[UserActivityPerDocument])