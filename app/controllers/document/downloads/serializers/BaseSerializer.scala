package controllers.document.downloads.serializers

import models.annotation.{Annotation, AnnotationBody}

trait BaseSerializer {

  protected def sortByCharOffset(annotations: Seq[Annotation]) =
    annotations.sortWith { (a, b) =>
      a.anchor.substring(12).toInt < b.anchor.substring(12).toInt
    }
  
  protected def getFirstQuote(a: Annotation): Option[String] = 
    a.bodies.find(_.hasType == AnnotationBody.QUOTE).flatMap(_.value)

  protected def getFirstEntityBody(a: Annotation): Option[AnnotationBody] = {
    import AnnotationBody._
    a.bodies.find(b => b.hasType == PERSON || b.hasType == PLACE )
  }

}