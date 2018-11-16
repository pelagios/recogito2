package controllers.my.ng.directory.list

/** Marker trait for Folder and Document directory list items **/
trait ListItem 

sealed class ListItemType

object ListItem {
  case object DOCUMENT extends ListItemType
  case object FOLDER extends ListItemType  
}