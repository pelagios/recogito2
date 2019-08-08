package services.contribution.feed.user

import org.joda.time.DateTime

case class UserActivityPerDay(timestamp: DateTime, count: Long, users: Seq[ActivityPerUser])