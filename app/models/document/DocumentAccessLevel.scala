package models.document

import play.api.libs.json.JsPath

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
  implicit val documentAccessLevelReads = JsPath.read[String].map(DocumentAccessLevel.withName(_).get)

}