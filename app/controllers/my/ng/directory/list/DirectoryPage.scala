package controllers.my.ng.directory.list

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
  items: Seq[ListItem]) {

  val page = Page(took, total, offset, limit, items)

}

object DirectoryPage {

  implicit val directoryPageWrites = new Writes[DirectoryPage] {

    def writes(p: DirectoryPage): JsValue =
      Json.toJson(p.page).as[JsObject] ++ 
      Json.obj("breadcrumbs" -> p.breadcrumbs.map { b => 
        Json.obj("title" -> b.title, "id" -> b.id)
      })
  }

}



