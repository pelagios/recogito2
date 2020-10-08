package controllers.api.annotation.helpers

import scala.concurrent.{ExecutionContext, Future}
import services.annotation.{Annotation, AnnotationBody, AnnotationService}

trait AnnotationValidator {

  // Shorthand
  private def hasRelations(annotation: Annotation) =
    annotation.relations.size > 0

  private def allRelationsValid(
    annotation: Annotation
  )(implicit 
      annotationService: AnnotationService,
      ctx: ExecutionContext
  ): Future[Boolean] =
    if (annotation.relations.size == 0) {
      Future.successful(true)
    } else {
      // Check if all destination annotations exist
      val destinationIds = 
        annotation.relations.map(_.relatesTo)

      annotationService.findByIds(destinationIds).map { destinations => 
        // Same number of annotations as destination Ids?
        destinations.flatten.size == destinationIds.size
      }
    }

  def isValidUpdate(
    annotation: Annotation, 
    previousVersion: Option[Annotation]
  )(implicit
      annotationService: AnnotationService,
      ctx: ExecutionContext
  ): Future[Boolean] = {
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

        // Anchors are only allowed to change on images and maps (= modified shape)
        val isValidAnchor = 
          annotation.annotates.contentType.isImage || 
            annotation.annotates.contentType.isMap ||
              annotation.anchor == previous.anchor

        // Finally, check if all relations point to valid destinations
        allRelationsValid(annotation).map { 
          _ && isSameId && isSameContentType && isSameFilepart && isValidAnchor
        }

      case None => 
        // A new insert is always a valid update
        Future.successful(true)
    }
  }

}