package controllers.document.annotation

import services.annotation.Annotation

case class AnnotationSummary(title: String, description: String)

object AnnotationSummary {
  
  // TODO
  def from(a: Annotation): AnnotationSummary = AnnotationSummary("", "")
  
}