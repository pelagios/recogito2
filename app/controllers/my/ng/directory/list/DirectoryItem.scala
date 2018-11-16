package controllers.my.ng.directory.list

/** Marker trait for Folder and Document directory list items **/
trait DirectoryItem 

sealed class DirectoryItemType

object DirectoryItem {
  case object DOCUMENT extends DirectoryItemType
  case object FOLDER extends DirectoryItemType  
}