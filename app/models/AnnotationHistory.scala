package models

import java.util.{ Date, UUID }

case class AnnotationHistory(

  annotationId: UUID,

  history: Seq[Annotation]

)
