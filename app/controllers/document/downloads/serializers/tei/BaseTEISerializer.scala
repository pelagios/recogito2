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
  
}