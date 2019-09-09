package services.annotation

// A pair of matching annotations that should be merged
case class MatchedPair(my: Annotation, toMerge: Annotation) {

  // TODO merge the individual bits of this matched pair
  def merge() = ???

}

trait HasAnnotationMerging {

  private def isMatch(a: Annotation, b: Annotation): Boolean = {
    // Shorthand
    def typeA = a.annotates.contentType
    def typeB = b.annotates.contentType
    
    if (typeA != typeB) {
      false  // Safe to abort here
    } else {
      if (a.anchor == b.anchor) {
        // In any case: if the anchor is the same, the annotations 
        // should be merged. Text & table annotations never change their
        // anchor, so this criterion is extra stable for them.
        true 
      } else {
        if (typeA.isImage) {
          // TODO make criterion smarter for images
          // 1. check if the shape has the same type
          // 2. check if the location/area is "vaguely" the same (area? centroid? thresholds?)

          // Temporary only, until we have smarter computation
          false
        } else {
          false // Anchors are not equal, and this is a text or table annotation -> no match
        }
      }
    }
  }

  /** Infers which annotations might have a common root and should be merged.
    * 
    * When cloning a document, connections between individual annotations are lost. 
    * This method tries to determine which annotation on the cloned document might
    * refer which annotation on the document-to-be-merged.
    *
    * Note: the "inference" is based on the annotation anchor. For text documents, this 
    * is a stable mechanism. For images it will result in duplicate annotations if an 
    * annotation shape was modified in the doc-to-be-merged. 
    *
    * Should we have a built-in connection between annotations on cloned documents?
    * Likely, this would only incur additional code and indexing overhead, while being 
    * just as messy...
    */
  def buildMergeSet(myAnnotations: Seq[Annotation], toMerge: Seq[Annotation]) = {
    // Split the 'toMerge' array in two parts:

    // 1. the annotations that are fresh additions - they should just be 
    //    added to my document    
    // 2. annotations that are changes to annotations that already exist 
    //    on my document - my annotations should be replaced by merged versions
    val (freshAdditions, haveMatch) = 
      toMerge.foldLeft((Seq.empty[Annotation], Seq.empty[MatchedPair])) { case ((freshAdditions, haveMatch), next) => 
        myAnnotations.find(a => isMatch(a, next)) match {
          case Some(matchingAnnotation) => 
            (freshAdditions, haveMatch :+ MatchedPair(matchingAnnotation, next))

          case None => 
            (freshAdditions :+ next, haveMatch)
        }
      }

    // TODO merge the matched pairs and return the list of annotations, so the service can upsert

  }

}
