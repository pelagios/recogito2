package controllers.my.ng

import play.api.libs.json._
import play.api.libs.functional.syntax._

object SortOrder extends Enumeration {
  type Order = Value
  val ASC, DESC = Value
}

case class Sorting(sortBy: String, order: SortOrder.Order)

object Sorting {

  implicit val sortingReads: Reads[Sorting] = (
    (JsPath \ "by").read[String] and
    (JsPath \ "asc").readNullable[Boolean].map(_ match {
      case Some(false) => SortOrder.DESC
      case _ => SortOrder.ASC // Default
    })
  )(Sorting.apply _)

}

case class PresentationConfig(columns: Seq[String], sort: Option[Sorting])

object PresentationConfig {

  implicit val presentationConfigReads: Reads[PresentationConfig] = (
    (JsPath \ "columns").readNullable[Seq[String]].map(_.getOrElse(Seq.empty[String])) and
    (JsPath \ "sort").readNullable[Sorting]
  )(PresentationConfig.apply _)

}