package controllers.my.ng.directory.list

import controllers.my.ng.directory.list.document.ConfiguredPresentation
import controllers.my.ng.directory.list.folder.FolderItem
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.Page

/** Marker trait for Folder and Document directory list items **/
trait ListItem 

sealed class ListItemType

object ListItem {
  case object DOCUMENT extends ListItemType
  case object FOLDER extends ListItemType  

  // TODO really just a hack for now...
  def merge(folders: Page[FolderItem], documents: Page[ConfiguredPresentation]) = {    
    val took = Math.max(folders.took, documents.took)
    val total = folders.total + documents.total
    
    val items = folders.items ++ documents.items
    Page(took, total, folders.offset, folders.limit, items)
  }

  implicit val listItemWrites = new Writes[ListItem] {

    def writes(item: ListItem): JsValue = item match {
      case i: FolderItem => Json.toJson(i)
      case i: ConfiguredPresentation => Json.toJson(i)
    }

  }

}