package services.contribution.feed.document

import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.contribution.{ContributionAction, ItemType}

/** Base unit of entry in the document activity feed.
  * 
  * { action } { count } { item type }
  *
  * e.g. "Created  5 place bodies" or "Deleted 1 tag"
  */
case class DocumentActivityFeedEntry(action: ContributionAction.Value, itemType: ItemType.Value, count: Long)

object DocumentActivityFeedEntry {

  implicit val documentActivityFeedEntryWrites: Writes[DocumentActivityFeedEntry] = (
    (JsPath \ "action").write[ContributionAction.Value] and
    (JsPath \ "item_type").write[ItemType.Value] and
    (JsPath \ "contributions").write[Long]
  )(unlift(DocumentActivityFeedEntry.unapply))

}