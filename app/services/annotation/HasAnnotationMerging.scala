package services.annotation

case class MergeSet(toAdd: Seq[Annotation], toReplace: Seq[Annotation])

trait HasAnnotationMerging {

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
  def inferCorrespondence(myAnnotations: Seq[Annotation], toMerge: Seq[Annotation]) = {

    // TODO
    ???
    
  }

}
