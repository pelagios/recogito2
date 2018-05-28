package controllers.document.downloads.serializers.tei

import controllers.document.downloads.serializers.BaseSerializer
import services.annotation.{ Annotation, AnnotationBody }

trait BaseTEISerializer extends BaseSerializer {

  /** By convention, use all tags starting with @ as XML attributes **/
  def getAttributes(annotation: Annotation) =
    annotation.bodies.filter { body =>
      body.hasType == AnnotationBody.TAG && 
      body.value.map { value => 
        value.startsWith("@") && 
        value.contains(':')
      }.getOrElse(false)
    }.map { body => 
      val tag = body.value.get
      val key = tag.substring(1, tag.indexOf(':'))
      val value = tag.substring(tag.indexOf(':') + 1)
      (key, value)
    }.groupBy { _._1 }.mapValues { _.map(_._2) }.toSeq
    
  
  /** Generates a <listRelation> element for relations, if any are contained in the annotations **/
  def relationsToList(annotations: Seq[Annotation]) = 
    annotations.filter(_.relations.size > 0) match {
      case Seq() => None // Empty list
      
      case sourceAnnotations =>
        val relationElements = sourceAnnotations.flatMap { source =>
          source.relations.map { relation =>
            val name = relation.bodies.map(_.value.replaceAll(" ", "_")).mkString
            val active = source.annotationId.toString
            val passive = relation.relatesTo.toString
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