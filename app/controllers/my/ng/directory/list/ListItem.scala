package controllers.my.ng.directory.list

import controllers.my.ng.directory.list.document.ConfiguredPresentation
import controllers.my.ng.directory.list.folder.FolderItem
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.Page
import services.folder.Breadcrumb

/** Marker trait for Folder and Document directory list items **/
trait ListItem 

sealed class ListItemType

object ListItem {
  case object DOCUMENT extends ListItemType
  case object FOLDER extends ListItemType  

  /** Concatenates a folder- and a document-list result **/
  def concat(breadcrumbs: Seq[Breadcrumb], folders: Page[FolderItem], documents: Page[ConfiguredPresentation]) = {    
    DirectoryPage(
      folders.took + documents.took, 
      folders.total + documents.total,
      folders.offset, 
      folders.limit,
      breadcrumbs,
      folders.items ++ documents.items)
  }

  implicit val listItemWrites = new Writes[ListItem] {

    def writes(item: ListItem): JsValue = item match {
      case i: FolderItem => Json.toJson(i)
      case i: ConfiguredPresentation => Json.toJson(i)
    }

  }

}