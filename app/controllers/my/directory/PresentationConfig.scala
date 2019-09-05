package controllers.my.directory

import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.SortOrder

case class Sorting(sortBy: String, order: SortOrder)

object Sorting {

  implicit val sortingReads: Reads[Sorting] = (
    (JsPath \ "by").read[String] and
    (JsPath \ "asc").readNullable[Boolean].map(_ match {
      case Some(false) => SortOrder.DESC
      case _ => SortOrder.ASC // Default
    })
  )(Sorting.apply _)

}

case class PresentationConfig(columns: Seq[String], sort: Option[Sorting]) {

  import PresentationConfig._

  // True if columns include fields that reside in the ES index
  lazy val containsIndexFields = columns.find(INDEX_FIELDS.contains(_))

  // True if columns include fields that reside in the DB
  lazy val containsDBFields = columns.find(DB_FIELDS.contains(_))

  // Shorthands
  def hasColumn(field: String) = columns.contains(field)

  def hasAnyColumn(fields: Seq[String]) = fields.intersect(columns).size > 0

}

object PresentationConfig {

  val INDEX_FIELDS = Seq(
    "last_edit_at",
    "last_edit_by",
    "annotations",
    "status_ratio",
    "activity")

  val DB_FIELDS = Seq(
    "owner",
    "uploaded_at",
    "title",
    "author",
    "date_freeform",
    "language",
    "is_public",
    "shared_by",
    "access_level",
    "cloned_from")

  implicit val presentationConfigReads: Reads[PresentationConfig] = (
    (JsPath \ "columns").readNullable[Seq[String]].map(_.getOrElse(Seq.empty[String])) and
    (JsPath \ "sort").readNullable[Sorting]
  )(PresentationConfig.apply _)

}