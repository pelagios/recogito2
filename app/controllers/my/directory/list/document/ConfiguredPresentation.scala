package controllers.my.directory.list.document

import controllers.my.directory.list.DirectoryItem
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.{ContentType, HasDate, Page}
import services.annotation.stats.StatusRatio
import services.document.{AccessibleDocument, SharedDocument}
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord, SharingPolicyRecord}

case class IndexDerivedProperties(
  lastEditAt: Option[DateTime],
  lastEditBy: Option[String],
  annotations: Option[Long],
  statusRatio: Option[StatusRatio]
)

object IndexDerivedProperties {

  // Shorthand
  val EMPTY = IndexDerivedProperties(None, None, None, None)

}

case class ConfiguredPresentation(
  document: DocumentRecord, 
  fileCount: Int,
  contentTypes: Seq[ContentType],
  sharedVia: Option[SharingPolicyRecord],
  indexProps: IndexDerivedProperties,
  columnConfig: Seq[String]
) extends DirectoryItem {

  // Helper to get the value of a DB property, under consideration of the columConfig
  def getDBProp[T](key: String, prop: T): Option[T] = 
    if (columnConfig.contains(key)) Option(prop) else None

  def getOptDBProp[T](key: String, prop: Option[T]): Option[T] =
    if (columnConfig.contains(key)) prop else None

  // The only difference for index props it that they are already properly Option-typed
  def getIndexProp[T](key: String, prop: Option[T]): Option[T] =
    if (columnConfig.contains(key)) prop else None

}

object ConfiguredPresentation extends HasDate {

  val DEFAULT_CONFIG = Seq("author", "title", "uploaded_at", "date_freeform")

  /** A helper to get the properties for the given doc from the list **/
  private def findProps(document: DocumentRecord, indexProps: Option[Map[String, IndexDerivedProperties]]) =
    indexProps.flatMap { p =>
      p.get(document.getId)
    }.getOrElse(IndexDerivedProperties.EMPTY)

  /** Builds a configured presentation.
    * 
    * Takes a DB search result, index-derived props and the column config 
    * as input. Column config is optional and will default to a sensible value.
    */
  def forMyDocument(
    page: Page[(DocumentRecord, Seq[DocumentFilepartRecord])],
    indexProps: Option[Map[String, IndexDerivedProperties]],
    columnConfig: Option[Seq[String]]
  ) = {
    val config = columnConfig.getOrElse(DEFAULT_CONFIG)
    page.map { case (document, parts) =>
      val props = findProps(document, indexProps)
      ConfiguredPresentation(
        document, 
        parts.size, 
        parts.flatMap(p => ContentType.withName(p.getContentType)).distinct,
        None, 
        props, 
        config)
    }
  }

  def forSharedDocument(
    page: Page[SharedDocument],
    indexProps: Option[Map[String, IndexDerivedProperties]],
    columnConfig: Option[Seq[String]]
  ) = {
    val config = columnConfig.getOrElse(DEFAULT_CONFIG)
    page.map { doc => 
      val props = findProps(doc.document, indexProps)
      ConfiguredPresentation(
        doc.document,
        doc.fileCount,
        doc.contentTypes,
        Some(doc.sharedVia),
        props,
        config)
    }
  }

  def forAccessibleDocument(
    page: Page[AccessibleDocument],
    indexProps: Option[Map[String, IndexDerivedProperties]],
    columnConfig: Option[Seq[String]]
  ) = {
    val config = columnConfig.getOrElse(DEFAULT_CONFIG)
    page.map { doc => 
      val props = findProps(doc.document, indexProps)
      ConfiguredPresentation(
        doc.document,
        doc.fileCount,
        doc.contentTypes,
        doc.sharedVia,
        props,
        config)
    }
  }

  implicit val configuredPresentationWrites: Writes[ConfiguredPresentation] = (
    // Write mandatory properties in any case
    (JsPath \ "type").write[String] and
    (JsPath \ "id").write[String] and
    (JsPath \ "owner").write[String] and 
    (JsPath \ "uploaded_at").write[DateTime] and
    (JsPath \ "title").write[String] and

    (JsPath \ "filetypes").write[Seq[String]] and
    (JsPath \ "file_count").write[Int] and

    // Selectable DB properties
    (JsPath \ "author").writeNullable[String] and 
    (JsPath \ "date_freeform").writeNullable[String] and
    (JsPath \ "language").writeNullable[String] and
    (JsPath \ "source").writeNullable[String] and
    (JsPath \ "edition").writeNullable[String] and
    (JsPath \ "public_visibility").writeNullable[String] and
    (JsPath \ "shared_by").writeNullable[String] and
    (JsPath \ "access_level").writeNullable[String] and

    // Selectable index properties
    (JsPath \ "last_edit_at").writeNullable[DateTime] and
    (JsPath \ "last_edit_by").writeNullable[String] and
    (JsPath \ "annotations").writeNullable[Long] and
    (JsPath \ "status_ratio").writeNullable[JsObject]
  )(p => (
    DirectoryItem.DOCUMENT.toString,
    p.document.getId,
    p.document.getOwner,
    new DateTime(p.document.getUploadedAt.getTime),
    p.document.getTitle,

    p.contentTypes.map(_.toString),
    p.fileCount,

    // DB-based props, based on whether they are defined in the column config
    p.getDBProp[String]("author", p.document.getAuthor),
    p.getDBProp[String]("date_freeform", p.document.getDateFreeform),
    p.getDBProp[String]("language", p.document.getLanguage),
    p.getDBProp[String]("source", p.document.getSource),
    p.getDBProp[String]("edition", p.document.getEdition),
    p.getDBProp[String]("public_visibility", p.document.getPublicVisibility),
    p.getOptDBProp[String]("shared_by", p.sharedVia.map(_.getSharedBy)),
    p.getOptDBProp[String]("access_level", p.sharedVia.map(_.getAccessLevel)),

    // Index-based properties
    p.getIndexProp[DateTime]("last_edit_at", p.indexProps.lastEditAt),
    p.getIndexProp[String]("last_edit_by", p.indexProps.lastEditBy),
    p.getIndexProp[Long]("annotations", p.indexProps.annotations),
    p.getIndexProp[StatusRatio]("status_ratio", p.indexProps.statusRatio).map { r =>
      Json.obj(
        "verified" -> r.verified,
        "unverified" -> r.unverified,
        "not_identifiable" -> r.notIdentifiable)
    }
  ))

}
