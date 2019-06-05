package controllers.my.directory.list.folder

import controllers.my.directory.list.DirectoryItem
import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.generated.tables.records.{FolderRecord, SharingPolicyRecord}

case class FolderItem(
  folder: FolderRecord,
  subfolderCount: Int,
  sharedVia: Option[SharingPolicyRecord] = None
) extends DirectoryItem 

object FolderItem {

implicit val folderItemWrites: Writes[FolderItem] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "id").write[UUID] and
    (JsPath \ "title").write[String] and
    (JsPath \ "parent").writeNullable[UUID] and
    (JsPath \ "owner").write[String] and
    (JsPath \ "has_subfolders").writeNullable[Boolean]
  )(f => (
    DirectoryItem.FOLDER.toString,
    f.folder.getId,
    f.folder.getTitle,
    Option(f.folder.getParent),
    f.folder.getOwner,
    { if (f.subfolderCount > 0) Some(true) else None }
  ))

}
