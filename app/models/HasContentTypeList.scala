package models

import play.api.libs.json.{Json, JsValue }

/** JSON representation converts ContentType to an array [ MediaType, ContentType ].
  *
  * This way we can use it for analytics more easily. E.g. 'TEXT_PLAIN' -> [ 'TEXT', 'TEXT_PLAIN' ] 
  */
trait HasContentTypeList {
    
  /** For convenience, this method accepts JSON strings as well as string arrays **/
  def fromCTypeList(typeOrList: JsValue): ContentType = {
    typeOrList.asOpt[Seq[String]] match {
      case Some(list) => list.flatMap(ContentType.withName(_)).head
      case None => typeOrList.asOpt[String] match {
        case Some(string) => ContentType.withName(string).get
        case None => throw new Exception("Invalid JSON - malformed content type: " + typeOrList)
      }
    }
  }
    
  def toCTypeList(ctype: ContentType): JsValue =
    Json.toJson(Seq(ctype.media, ctype.name))
  
}