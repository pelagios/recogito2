package controllers.api.annotation.helpers

import services.annotation.{Annotation, AnnotationBody}

trait AnnotationValidator {

  def isValidUpdate(annotation: Annotation, previousVersion: Option[Annotation]): Boolean = {
    previousVersion match {
      case Some(previous) => 
        // ID needs to stay the same
        val isSameId = annotation.annotationId == previous.annotationId

        // Change of content type not allowed
        val isSameContentType =
          annotation.annotates.contentType == previous.annotates.contentType

        // Change of doc/filepart ID not allowed
        val isSameFilepart = 
          annotation.annotates.documentId == previous.annotates.documentId &&
          annotation.annotates.filepartId == previous.annotates.filepartId

        // Anchors are only allowed to change on images (= modified shape)
        val isValidAnchor = 
          annotation.annotates.contentType.isImage ||
            annotation.anchor == previous.anchor

        isSameId && isSameContentType && isSameFilepart && isValidAnchor

      case None => 
        // A new insert is always a valid update
        true
    }
  }

}