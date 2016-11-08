package models.visit

import java.util.UUID
import models.{ ContentType, HasContentTypeList, HasDate }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Visit(
    
  url: String,

  referer: Option[String],
   
  visitedAt: DateTime,
  
  client: Client,
  
  responseFormat: String,
  
  visitedItem: Option[VisitedItem]
  
)

object Visit extends HasDate {
  
  implicit val visitFormat: Format[Visit] = (
    (JsPath \ "url").format[String] and
    (JsPath \ "referer").formatNullable[String] and
    (JsPath \ "visited_at").format[DateTime] and
    (JsPath \ "client").format[Client] and
    (JsPath \ "response_format").format[String] and
    (JsPath \ "visited_item").formatNullable[VisitedItem]
  )(Visit.apply, unlift(Visit.unapply))
  
}


case class Client(
  
  ip: String,
  
  userAgent: String,
  
  browser: String,
  
  os: String,
  
  deviceType: String
  
)

object Client {
  
  implicit val clientFormat: Format[Client] = (
    (JsPath \ "ip").format[String] and
    (JsPath \ "user_agent").format[String] and
    (JsPath \ "browser").format[String] and
    (JsPath \ "os").format[String] and
    (JsPath \ "device_type").format[String]
  )(Client.apply, unlift(Client.unapply))
  
}


case class VisitedItem(
    
  documentId: String,
  
  documentOwner: String,
  
  filepartId: Option[UUID],
  
  contentType: Option[ContentType]

)

object VisitedItem extends HasContentTypeList {
  
  implicit val visitedItemFormat: Format[VisitedItem] = (
    (JsPath \ "document_id").format[String] and
    (JsPath \ "document_owner").format[String] and
    (JsPath \ "filepart_id").formatNullable[UUID] and
    (JsPath \ "content_type").formatNullable[JsValue]
      .inmap[Option[ContentType]](_.map(fromCTypeList), _.map(toCTypeList))
  )(VisitedItem.apply, unlift(VisitedItem.unapply))
  
}



