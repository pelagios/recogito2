package models.document

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

sealed trait DocumentAccessLevel {
  
  val canRead: Boolean
  
  val canWrite: Boolean
  
}

object DocumentAccessLevel {

  case object FORBIDDEN extends DocumentAccessLevel { val canRead = false ; val canWrite = false }
  case object READ      extends DocumentAccessLevel { val canRead = true  ; val canWrite = false }
  case object WRITE     extends DocumentAccessLevel { val canRead = true  ; val canWrite = true  }
  case object ADMIN     extends DocumentAccessLevel { val canRead = true  ; val canWrite = true  }
  case object OWNER     extends DocumentAccessLevel { val canRead = true  ; val canWrite = true  }
  
  def withName(name: String): Option[DocumentAccessLevel] =
    Seq(FORBIDDEN, READ, WRITE, ADMIN, OWNER).find(_.toString == name)
   
  // JSON serialization
  implicit val documentAccessLevelFromat: Format[DocumentAccessLevel] = 
    Format(
      JsPath.read[String].map(DocumentAccessLevel.withName(_).get),
      Writes[DocumentAccessLevel](l => JsString(l.toString))
    )

}