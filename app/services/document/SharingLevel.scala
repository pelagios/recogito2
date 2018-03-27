package services.document

import play.api.libs.json._
import play.api.libs.functional.syntax._

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
  
}
