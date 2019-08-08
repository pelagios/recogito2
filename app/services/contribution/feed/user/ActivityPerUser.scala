package services.contribution.feed.user

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class ActivityPerUser(username: String, count: Long, documents: Seq[UserActivityPerDocument])

object ActivityPerUser {

  implicit val activityPerUserWrites: Writes[ActivityPerUser] = (
    (JsPath \ "username").write[String] and
    (JsPath \ "contributions").write[Long] and 
    (JsPath \ "documents").write[Seq[UserActivityPerDocument]]
  )(unlift(ActivityPerUser.unapply))

}