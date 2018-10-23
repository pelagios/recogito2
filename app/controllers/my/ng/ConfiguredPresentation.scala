package controllers.my.ng

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasDate
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}

case class IndexDerivedProperties(
  lastEditAt: Option[DateTime],
  lastEditBy: Option[String],
  annotations: Option[Long]
)

object IndexDerivedProperties {

  // Shorthand
  val EMPTY = IndexDerivedProperties(None, None, None)

}

case class ConfiguredPresentation(
  document: DocumentRecord, 
  parts: Seq[DocumentFilepartRecord],
  indexProps: IndexDerivedProperties,
  columnConfig: Seq[String]
) {

  import ConfiguredPresentation._

  def getIfDefined[T](field: String): Option[T] = field match {
    
    // Standards Option[String] DB fields
    case s if OPT_STRING_DBFIELDS.contains(s) => 
      if (columnConfig.contains(s)) prop else None

    // Other DB fields
    case "is_public" => None // TODO

    // Index-based fields: last_edit_at, last_edit_by, annotations
  }

}

object ConfiguredPresentation extends HasDate {

  val OPT_STRING_DBFIELDS = 
    Seq("author", "date_freeform", "language", "source", "edition")

  implicit val configuredPresentationWrites: Writes[ConfiguredPresentation] = (
    // Write mandatory properties in any case
    (JsPath \ "id").write[String] and
    (JsPath \ "owner").write[String] and 
    (JsPath \ "uploaded_at").write[DateTime] and
    (JsPath \ "title").write[String] and

    // Selectable DB properties
    (JsPath \ "author").writeNullable[String] and 
    (JsPath \ "date_freeform").writeNullable[String] and
    // TODO description?
    (JsPath \ "language").writeNullable[String] and
    (JsPath \ "source").writeNullable[String] and
    (JsPath \ "edition").writeNullable[String] and
    (JsPath \ "is_public").writeNullable[Boolean] and

    // Selectable index properties
    (JsPath \ "last_edit_at").writeNullable[DateTime] and
    (JsPath \ "last_edit_by").writeNullable[String] and
    (JsPath \ "annotations").writeNullable[Long]
  )(p => (
    p.document.id,
    p.document.getOwner,
    p.document.getUploadedAt,
    p.document.getTitle,

    p.getIfDefined[String]("author"),
    p.getIfDefined[String]("date_freeform"),
    p.getIfDefined[String]("language"),
    p.getIfDefined[String]("source"),
    p.getIfDefined[String]("edition"),
    p.getIfDefined[Boolean]("is_public"),

    p.getIfDefined[DateTime]("last_edit_at"),
    p.getIfDefined[String]("last_edit_by"),
    p.getIfDefined[Long]("annotations")
  ))

}
