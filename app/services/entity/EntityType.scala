package services.entity

import play.api.libs.json._
import play.api.libs.functional.syntax._

sealed trait EntityType

object EntityType {
  
  object PLACE  extends EntityType { override val toString = "PLACE" }
  object PERSON extends EntityType { override val toString = "PERSON" }
  
  private val all = Seq(PLACE, PERSON)
  
  def withName(s: String) =
    all.find(_.toString == s.toUpperCase).get
    
  implicit val entityTypeFormat: Format[EntityType] = Format(
    JsPath.read[JsString].map(json => withName(json.value)),
    Writes[EntityType](t => Json.toJson(t.toString))
  )
    
}