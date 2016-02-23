package models

import java.util.{ Date, UUID }

case class Annotation(

  annotationId: UUID,

  versionId: UUID,

  annotates: AnnotatedObject,

  hasPreviousVersions: Int,

  contributors: Seq[String],

  anchor: String,

  createdBy: String,

  createdAt: Date,

  lastModifiedBy: String,

  lastModifiedAt: Date,

  bodies: Seq[AnnotationBody],

  status: AnnotationStatus

)

case class AnnotatedObject(document: Integer, filepart: Integer)
