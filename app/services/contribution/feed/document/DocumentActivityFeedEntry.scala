package services.contribution.feed.document

import services.contribution.{ContributionAction, ItemType}

/** Base unit of entry in the document activity feed.
  * 
  * { action } { count } { item type }
  *
  * e.g. "Created  5 place bodies" or "Deleted 1 tag"
  */
case class DocumentActivityFeedEntry(action: ContributionAction.Value, itemType: ItemType.Value, count: Long)