package controllers.my.ng.directory.list

import controllers.my.ng.directory.list.document.ConfiguredPresentation
import controllers.my.ng.directory.list.folder.FolderItem
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** Marker trait for Folder and Document directory list items **/
trait DirectoryItem 

sealed class DirectoryItemType

object DirectoryItem {
  case object DOCUMENT extends DirectoryItemType
  case object FOLDER extends DirectoryItemType  

  implicit val directoryItemWrites = new Writes[DirectoryItem] {

    def writes(item: DirectoryItem): JsValue = item match {
      case i: FolderItem => Json.toJson(i)
      case i: ConfiguredPresentation => Json.toJson(i)
    }

  }

}