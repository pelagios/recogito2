package controllers.my.ng.directory.list.folder

import controllers.my.ng.directory.list.ListItem
import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.generated.tables.records.FolderRecord

case class FolderItem(folder: FolderRecord) extends ListItem 

object FolderItem {

implicit val folderItemWrites: Writes[FolderItem] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "id").write[UUID] and
    (JsPath \ "title").write[String] and
    (JsPath \ "parent").writeNullable[UUID] and
    (JsPath \ "owner").write[String] 
  )(f => (
    ListItem.FOLDER.toString,
    f.folder.getId,
    f.folder.getTitle,
    Option(f.folder.getParent),
    f.folder.getOwner
  ))

}
