package controllers.document.downloads.serializers

import models.annotation.{ Annotation, AnnotationBody }

trait BaseSerializer {
  
  protected def getFirstQuote(a: Annotation): Option[String] = 
    a.bodies.filter(_.hasType == AnnotationBody.QUOTE).headOption.flatMap(_.value)

}