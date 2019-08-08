package services.contribution.feed.user

import services.ContentType
import services.contribution.{ContributionAction, ItemType}

/** Base unit of entry in the user activity feed.
  * 
  * { action } { count } { item type } { content type}
  *
  * e.g. "Created  5 place bodies on image" or "Deleted 1 tag on TEI"
  */
case class UserActivityFeedEntry(
  action: ContributionAction.Value, 
  itemType: ItemType.Value, 
  contentType: ContentType, 
  count: Long)