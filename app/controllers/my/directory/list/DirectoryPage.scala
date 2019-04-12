package controllers.my.directory.list

import controllers.my.directory.ConfiguredPresentation
import controllers.my.directory.list.folder.FolderItem
import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.Page
import services.folder.Breadcrumb

case class DirectoryPage(
  took: Long, 
  total: Long, 
  offset: Int, 
  limit: Long,
  breadcrumbs: Seq[Breadcrumb],
  readme: Option[String],
  items: Seq[DirectoryItem]) {

  val page = Page(took, total, offset, limit, items)

}

object DirectoryPage {

  def build(
    readme: Option[String], 
    breadcrumbs: Seq[Breadcrumb], 
    folders: Page[FolderItem], 
    documents: Page[ConfiguredPresentation]
  ) = DirectoryPage(
    folders.took + documents.took, 
    folders.total + documents.total,
    folders.offset, 
    folders.limit,
    breadcrumbs,
    readme,
    folders.items ++ documents.items
  )

  implicit val breadcrumbWrites: Writes[Breadcrumb] = (
    (JsPath \ "id").write[UUID] and 
    (JsPath \ "title").write[String]
  )(unlift(Breadcrumb.unapply))

  implicit val directoryPageWrites: Writes[DirectoryPage] = (
    (JsPath).write[Page[DirectoryItem]] and
    (JsPath \ "breadcrumbs").write[Seq[Breadcrumb]] and
    (JsPath \ "readme").writeNullable[String] 
  )(p => (
    p.page,
    p.breadcrumbs,
    p.readme
  ))

}



