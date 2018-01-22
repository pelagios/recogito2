package services.annotation

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.language.implicitConversions

case class Reference(uri: String, unionId: Option[UUID] = None)

object Reference {
  
  implicit def toOptReference(maybeUri: Option[String]) = 
    maybeUri.map(Reference(_))
  
  implicit def toReference(uri: String) = 
    Reference(uri)

  implicit val entityReferenceFormat: Format[Reference] = (
    (JsPath \ "uri").format[String] and
    (JsPath \ "union_id").formatNullable[UUID]
  )(Reference.apply, unlift(Reference.unapply))
  
}