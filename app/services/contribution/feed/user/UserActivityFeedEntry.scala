package services.contribution.feed.user

import play.api.libs.json._
import play.api.libs.functional.syntax._
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

object UserActivityFeedEntry {

  implicit val userActivityFeedEntryWrites: Writes[UserActivityFeedEntry] = (
    (JsPath \ "action").write[ContributionAction.Value] and
    (JsPath \ "item_type").write[ItemType.Value] and
    (JsPath \ "content_type").write[ContentType] and
    (JsPath \ "contributions").write[Long]
  )(unlift(UserActivityFeedEntry.unapply))

}