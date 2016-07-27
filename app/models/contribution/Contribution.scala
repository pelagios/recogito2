package models.contribution

import models.{ HasDate, HasNullableSeq }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Contribution (
  
  action: ContributionAction.Value,
  
  madeBy: String,
  
  madeAt: DateTime,
  
  affectsItem: Item,
  
  affectsUsers: Seq[String],
  
  context: Option[String]

)

object Contribution extends HasDate with HasNullableSeq {
  
  /** JSON conversion **/
  implicit val contributionFormat: Format[Contribution] = (
    (JsPath \ "action").format[ContributionAction.Value] and
    (JsPath \ "made_by").format[String] and
    (JsPath \ "made_at").format[DateTime] and
    (JsPath \ "affects_item").format[Item] and
    (JsPath \ "affects_users").formatNullable[Seq[String]]
      .inmap[Seq[String]](fromOptSeq[String], toOptSeq[String]) and
    (JsPath \ "context").formatNullable[String]
  )(Contribution.apply, unlift(Contribution.unapply))
  
}
