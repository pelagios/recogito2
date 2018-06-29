package controllers.document.downloads.serializers.tei

import controllers.document.downloads.serializers.BaseSerializer
import java.util.UUID
import services.annotation.{ Annotation, AnnotationBody }

trait BaseTEISerializer extends BaseSerializer {
  
  /** Creates an "ID" that conforms to TEI restrictions **/
  def toTeiId(uuid: UUID): String = s"recogito-${uuid}"
  
  def getAttribute(tag: String) = {
    val sepIdx =
      if (tag.count(_ == ':') == 1)
        tag.indexOf(':')
      else 
        tag.indexOf(':', tag.indexOf(':') + 1)
      
    val key = tag.substring(1, sepIdx)
    val value = tag.substring(sepIdx + 1)
    (key, value)
  }
  
  /** Checks if this tag should be treated as XML attribute.
    * 
    * By convention, tags of the form '@key:value' are serialized
    * as xml attribute, e.g. <span key="value">.
    */
  def isAttributeTag(body: AnnotationBody) =
    body.value.map { value =>
      value.startsWith("@") &&
      value.contains(':')
    }.getOrElse(false)
      
  /** By convention, use all tags starting with @ as XML attributes **/
  def getAttributeTags(annotation: Annotation) =
    annotation.bodies.filter { body =>
      body.hasType == AnnotationBody.TAG && isAttributeTag(body)
    }.map { body => 
      getAttribute(body.value.get)
    }.groupBy { _._1 }.mapValues { _.map(_._2) }.toSeq
    
  /** All tags that don't fall into the 'attribute tag' convetion above **/
  def getNonAttributeTags(annotation: Annotation) = 
    annotation.bodies.filter { body =>
      body.hasType == AnnotationBody.TAG && !isAttributeTag(body)
    }.map { _.value.get }
    
  /** Generates a <listRelation> element for relations, if any are contained in the annotations **/
  def relationsToList(annotations: Seq[Annotation]) = 
    annotations.filter(_.relations.size > 0) match {
      case Seq() => None // Empty list
      
      case sourceAnnotations =>
        val relationElements = sourceAnnotations.flatMap { source =>
          source.relations.map { relation =>
            val name = relation.bodies.map(_.value.replaceAll(" ", "_")).mkString
            val active = toTeiId(source.annotationId)
            val passive = toTeiId(relation.relatesTo)
            <relation name={name} active={active} passive={passive} />
          }
        }
        
        Some(<ab>
          <listRelation>
            {relationElements}
          </listRelation>
        </ab>)
    }
  
}