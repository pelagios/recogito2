package services.contribution

import java.util.UUID
import services.{ ContentType, HasContentTypeList }
import services.annotation.AnnotationBody
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasContentTypeList

case class Item(
  
  itemType: ItemType.Value,
  
  documentId: String,
  
  documentOwner: String,
  
  filepartId: Option[UUID],
  
  contentType: Option[ContentType],
  
  annotationId: Option[UUID],
  
  annotationVersionId: Option[UUID],
  
  valueBefore: Option[String],
  
  valueAfter: Option[String]
  
)

object Item extends HasContentTypeList {
  
  implicit val itemFormat: Format[Item] = (
    (JsPath \ "item_type").format[ItemType.Value] and
    (JsPath \ "document_id").format[String] and
    (JsPath \ "document_owner").format[String] and
    (JsPath \ "filepart_id").formatNullable[UUID] and
    (JsPath \ "content_type").formatNullable[JsValue]
      .inmap[Option[ContentType]](_.map(fromCTypeList), _.map(toCTypeList)) and
    (JsPath \ "annotation_id").formatNullable[UUID] and
    (JsPath \ "annotation_version_id").formatNullable[UUID] and
    (JsPath \ "value_before").formatNullable[String] and
    (JsPath \ "value_after").formatNullable[String]
  )(Item.apply, unlift(Item.unapply))

}
  
object ItemType extends Enumeration {

  val DOCUMENT = Value("DOCUMENT")

  val ANNOTATION = Value("ANNOTATION")

  val QUOTE_BODY = Value("QUOTE_BODY")

  val TRANSCRIPTION_BODY = Value("TRANSCRIPTION_BODY")

  val COMMENT_BODY = Value("COMMENT_BODY")
  
  val PLACE_BODY = Value("PLACE_BODY")
  
  val PERSON_BODY = Value("PERSON_BODY")
  
  val EVENT_BODY = Value("EVENT_BODY")
  
  val TAG_BODY = Value("TAG_BODY")
  
  val RELATION = Value("RELATION")
  
  val RELATION_TAG = Value("RELATION_TAG")

  // MRM extensions
  val ENTITY_BODY = Value("ENTITY_BODY")

  val LABEL_BODY = Value("LABEL_BODY")

  val SYMBOL_BODY = Value("SYMBOL_BODY")

  val GROUPING_BODY = Value("GROUPING_BODY")

  val ORDERING_BODY = Value("ORDERING_BODY")

  def fromBodyType(bodyType: AnnotationBody.Value) = bodyType match {

    case AnnotationBody.COMMENT => COMMENT_BODY

    case AnnotationBody.PLACE => PLACE_BODY

    case AnnotationBody.PERSON => PERSON_BODY

    case AnnotationBody.EVENT => EVENT_BODY

    case AnnotationBody.QUOTE => QUOTE_BODY

    case AnnotationBody.TRANSCRIPTION => TRANSCRIPTION_BODY
    
    case AnnotationBody.TAG => TAG_BODY

    // MRM extensions
    case AnnotationBody.ENTITY => ENTITY_BODY

    case AnnotationBody.LABEL => LABEL_BODY
    
    case AnnotationBody.SYMBOL => SYMBOL_BODY
    
    case AnnotationBody.GROUPING => GROUPING_BODY 

    case AnnotationBody.ORDERING => ORDERING_BODY

  }
      
  /** JSON conversion **/
  implicit val itemTypeFormat: Format[ItemType.Value] =
    Format(
      JsPath.read[String].map(ItemType.withName(_)),
      Writes[ItemType.Value](s => JsString(s.toString))
    )
 
}