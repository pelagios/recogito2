package services

import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.generated.tables.records.{DocumentRecord, FolderRecord, SharingPolicyRecord}

sealed trait SharingLevel

object SharingLevel {
  
  case object READ  extends SharingLevel
  case object WRITE extends SharingLevel
  case object ADMIN extends SharingLevel
  
  def withName(name: String) =
    Seq(READ, WRITE, ADMIN).find(_.toString == name)
    
  implicit val sharingLevelFormat: Format[SharingLevel] = 
    Format(
      JsPath.read[String].map(SharingLevel.withName(_).get),
      Writes[SharingLevel](l => JsString(l.toString))
    )

  object Utils {

    /** Returns true if the given user is the folder owner or has admin rights **/
    def isFolderAdmin(username: String, folder: FolderRecord, policy: Option[SharingPolicyRecord]) = 
      folder.getOwner == username ||
        policy.map { p => 
          p.getSharedWith == username && p.getAccessLevel == SharingLevel.ADMIN
        }.getOrElse(false)

    def isDocumentAdmin(username: String, document: DocumentRecord, policy: Option[SharingPolicyRecord]) =
      document.getOwner == username ||
        policy.map { p => 
          p.getSharedWith == username && p.getAccessLevel == SharingLevel.ADMIN
        }.getOrElse(false) // Has admin rights

  }
  
}
