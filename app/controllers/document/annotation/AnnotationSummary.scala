package controllers.document.annotation

import services.annotation.Annotation

case class AnnotationSummary(title: String, description: String)

object AnnotationSummary {
  
  def from(a: Annotation): AnnotationSummary = AnnotationSummary("foo", "bar baz")
  
}