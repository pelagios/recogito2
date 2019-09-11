package services.annotation

import java.util.UUID
import org.joda.time.DateTime

// A pair of matching annotations that should be merged
case class MatchedPair(my: Annotation, toMerge: Annotation) {

  def merge(): Annotation = {

    val contributors = (my.contributors ++ toMerge.contributors).distinct

    val lastModifiedAt = new DateTime(Seq(my.lastModifiedAt.getMillis, toMerge.lastModifiedAt.getMillis).max)
    val lastModifiedBy =
      if (lastModifiedAt == my.lastModifiedAt) my.lastModifiedBy
      else toMerge.lastModifiedBy

    val bodiesToAppend = toMerge.bodies.filter { body => 
      val matchingBody = my.bodies.find(_.equalsIgnoreModified(body))
      matchingBody.isEmpty
    }

    // Currently, the implementation allows only a single relation per path, 
    // each with a single body. When there are relations (in my and toMerge)
    // that have the same path, the newer relation will overwrite the older one.

    // Step 1: filter relations in 'toMerge' to those that don't have a 
    // correspondence in 'my' - we'll later append those
    val relationsToAppend = toMerge.relations.filter { relation => 
      val matchingRelation = my.relations.find(_.hasSamePath(relation))
      matchingRelation.isEmpty
    }

    // Step 2: Go through all relations in 'my' and replace them 
    // if there is a matching relation in 'toMerge' which is newer.
    val relations = my.relations.map { relation => 
      toMerge.relations.find(_.hasSamePath(relation)) match {
        case Some(matchingRelation) => 
          if (matchingRelation.lastModifiedAt.getMillis > relation.lastModifiedAt.getMillis)
            matchingRelation
          else 
            relation

        case None => relation
      }
    }

    Annotation(
      my.annotationId,
      UUID.randomUUID,
      my.annotates,
      contributors,
      my.anchor, // TODO what to do about "roughly" same anchors on images?
      lastModifiedBy,
      lastModifiedAt,
      my.bodies ++ bodiesToAppend,
      relations ++ relationsToAppend)
  }

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
  def buildMergeSet(
    myAnnotations: Seq[Annotation], 
    toMerge: Seq[Annotation], 
    destinationDoc: String,
    filepartMap: Map[UUID, UUID]
  ) = {
    play.api.Logger.info(filepartMap.toString)
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
            filepartMap.get(next.annotates.filepartId) match {
              case Some(partId) => 
                val cloned = next.cloneTo(destinationDoc, partId)
                (freshAdditions :+ cloned, haveMatch)

              case None => 
                (freshAdditions, haveMatch)
            }
        }
      }

    freshAdditions ++ haveMatch.map(_.merge)
  }

}
