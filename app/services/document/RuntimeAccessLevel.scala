package services.document

import play.api.libs.json._
import play.api.libs.functional.syntax._

/** Runtime privileges define what a particular user can do on a particluar document.
  *  
  * In general, these privileges are a result of evaluating a bunch of rules (during runtime):
  * - Is it the users own document?
  * - Does the document define any public visibility settings?
  * - Are there any sharing rules defined for this user on this document?
  * 
  * Runtime privileges provide the total aggregated result from this rule evaluation.
  */
sealed trait RuntimeAccessLevel {

  // Has access to annotations (incl. map, annotation-only downloads) and doc metadata
  val canReadData: Boolean
  
  // Has access to document content (text, image)
  val canReadAll: Boolean
  
  // Has write privileges
  val canWrite: Boolean
  
  // Has admin privileges
  val isAdmin: Boolean
  
}

object RuntimeAccessLevel {

  case object FORBIDDEN extends RuntimeAccessLevel 
    { val canReadData = false ; val canReadAll = false ; val canWrite = false ; val isAdmin = false }
  
  case object READ_DATA extends RuntimeAccessLevel 
    { val canReadData = true  ; val canReadAll = false ; val canWrite = false ; val isAdmin = false }

  case object READ_ALL extends RuntimeAccessLevel 
    { val canReadData = true  ; val canReadAll = true  ; val canWrite = false ; val isAdmin = false }
  
  case object WRITE extends RuntimeAccessLevel 
    { val canReadData = true  ; val canReadAll = true  ; val canWrite = true  ; val isAdmin = false }
  
  case object ADMIN extends RuntimeAccessLevel 
    { val canReadData = true  ; val canReadAll = true  ; val canWrite = true  ; val isAdmin = true  }
  
  case object OWNER extends RuntimeAccessLevel 
    { val canReadData = true  ; val canReadAll = true  ; val canWrite = true  ; val isAdmin = true  }
  
  def withName(name: String): Option[RuntimeAccessLevel] =
    Seq(FORBIDDEN, READ_DATA, READ_ALL, WRITE, ADMIN, OWNER).find(_.toString == name)
    
  def fromSharingLevel(sharingLevel: SharingLevel) = sharingLevel match {
    case SharingLevel.READ => RuntimeAccessLevel.READ_ALL
    case SharingLevel.WRITE => RuntimeAccessLevel.WRITE
    case SharingLevel.ADMIN => RuntimeAccessLevel.ADMIN
  }
  
  implicit val runtimeAccessLevelFormat: Format[RuntimeAccessLevel] = 
    Format(
      JsPath.read[String].map(RuntimeAccessLevel.withName(_).get),
      Writes[RuntimeAccessLevel](l => JsString(l.toString))
    )

}
