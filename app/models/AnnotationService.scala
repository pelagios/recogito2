package models

import play.api.Logger

object AnnotationService {

  def insertAnnotations(annotations: Seq[Annotation]) = {
    Logger.info("Not inserting annotations just yet...")
    annotations.foreach(annotation =>
      Logger.info(annotation.toString))
  }

}
