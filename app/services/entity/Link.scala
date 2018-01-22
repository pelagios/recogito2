package services.entity

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Link(uri: String, linkType: LinkType.Value) {
  
  def normalize = Link(EntityRecord.normalizeURI(uri), linkType)
  
}

object Link {
  
  implicit val linkFormat: Format[Link] = (
    (JsPath \ "uri").format[String] and
    (JsPath \ "link_type").format[LinkType.Value]
  )(Link.apply, unlift(Link.unapply))

}

object LinkType extends Enumeration {
  
  val CLOSE_MATCH = Value("closeMatch")
  
  val EXACT_MATCH = Value("exactMatch")
  
  implicit val linkTypeFormat: Format[LinkType.Value] =
    Format(
      JsPath.read[JsString].map(json => LinkType.withName(json.value)),
      Writes[LinkType.Value](l => Json.toJson(l.toString))
    )
    
}