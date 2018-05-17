package controllers

import services.ContentType
import services.annotation.{Annotation, AnnotationBody, AnnotationStatus}
import services.annotation.relation.Relation
import services.contribution._
import services.generated.tables.records.DocumentRecord

trait HasAnnotationValidation {
  
  /** The context field is a hint for the user in which... context... the contribution occured.
    *
    * For text annotations, we'll use the annotation's quote; for image annotations, the transcription.   
    */
  protected def getContext(annotation: Annotation) =
    annotation.bodies
      .filter(a =>
        a.hasType == AnnotationBody.QUOTE || a.hasType == AnnotationBody.TRANSCRIPTION)
      .headOption.flatMap(_.value)

  private def createBodyContribution(annotationAfter: Annotation, createdBody: AnnotationBody, document: DocumentRecord) =
    Contribution(
      ContributionAction.CREATE_BODY,
      annotationAfter.lastModifiedBy.get,
      annotationAfter.lastModifiedAt,
      Item(
        ItemType.fromBodyType(createdBody.hasType),
        annotationAfter.annotates.documentId,
        document.getOwner,
        Some(annotationAfter.annotates.filepartId),
        Some(annotationAfter.annotates.contentType),
        Some(annotationAfter.annotationId),
        Some(annotationAfter.versionId),
        None,
        // At least currently, bodies have either value or URI - never both
        if (createdBody.value.isDefined) createdBody.value else createdBody.uri
      ),
      Seq.empty[String],
      getContext(annotationAfter)
    )

  /** Changes to bodies are either general 'edits' or status changes (confirmations or flags) **/
  private def determineChangeAction(bodyBefore: AnnotationBody, bodyAfter: AnnotationBody) = {
    
    import AnnotationStatus._
    import ContributionAction._
    
    if (bodyAfter.status != bodyBefore.status) {
      bodyAfter.status.get.value match {
        case VERIFIED         => CONFIRM_BODY
        case NOT_IDENTIFIABLE => FLAG_BODY
        case UNVERIFIED       => EDIT_BODY // Something else was changed
      }     
    } else {
      EDIT_BODY
    }
  }

  private def changeBodyContribution(annotationBefore: Annotation, annotationAfter: Annotation, bodyBefore: AnnotationBody, bodyAfter: AnnotationBody, document: DocumentRecord) =
    Contribution(
      determineChangeAction(bodyBefore, bodyAfter),
      annotationAfter.lastModifiedBy.get,
      annotationAfter.lastModifiedAt,
      Item(
        ItemType.fromBodyType(bodyAfter.hasType),
        annotationAfter.annotates.documentId,
        document.getOwner,
        Some(annotationAfter.annotates.filepartId),
        Some(annotationAfter.annotates.contentType),
        Some(annotationAfter.annotationId),
        Some(annotationAfter.versionId),
        // At least currently, bodies have either value or URI - never both
        if (bodyBefore.value.isDefined) bodyBefore.value else bodyBefore.uri,
        if (bodyAfter.value.isDefined) bodyAfter.value else bodyAfter.uri
      ),
      if (bodyAfter.lastModifiedBy == bodyBefore.lastModifiedBy) Seq.empty[String] else Seq(bodyBefore.lastModifiedBy).flatten,
      getContext(annotationAfter)
    )

  private def deleteBodyContribution(annotationBefore: Annotation, annotationAfter: Annotation, deletedBody: AnnotationBody, document: DocumentRecord) =
    Contribution(
      ContributionAction.DELETE_BODY,
      annotationAfter.lastModifiedBy.get,
      annotationAfter.lastModifiedAt,
      Item(
        ItemType.fromBodyType(deletedBody.hasType),
        annotationAfter.annotates.documentId,
        document.getOwner,
        Some(annotationAfter.annotates.filepartId),
        Some(annotationAfter.annotates.contentType),
        Some(annotationAfter.annotationId),
        Some(annotationAfter.versionId),
        // At least currently, bodies have either value or URI - never both
        if (deletedBody.value.isDefined) deletedBody.value else deletedBody.uri,
        None
      ),
      if (deletedBody.lastModifiedBy == annotationAfter.lastModifiedBy) Seq.empty[String] else Seq(deletedBody.lastModifiedBy).flatten,
      getContext(annotationAfter)
    )

  /** At the moment, just checks for equal types, but may become more sophisticated in the future **/
  private def isPredecessorTo(before: AnnotationBody, after: AnnotationBody): Boolean =
    after.hasType == before.hasType
    
  /** TODO extend to relation-body granularity later **/
  private def createRelationContribution(annotationAfter: Annotation, createdRelation: Relation, document: DocumentRecord) =
    Contribution(
      ContributionAction.CREATE_RELATION_BODY,
      annotationAfter.lastModifiedBy.get,
      annotationAfter.lastModifiedAt,
      Item(
        ItemType.RELATION_TAG,
        annotationAfter.annotates.documentId,
        document.getOwner,
        Some(annotationAfter.annotates.filepartId),
        Some(annotationAfter.annotates.contentType),
        Some(annotationAfter.annotationId),
        Some(annotationAfter.versionId),
        None, // valueBefore
        Some(createdRelation.bodies.map(_.value).mkString)
      ),
      Seq.empty[String],
      getContext(annotationAfter)
    )
    
  private def changeRelationContribution(annotationBefore: Annotation, annotationAfter: Annotation, relationBefore: Relation, relationAfter: Relation, document: DocumentRecord) =
    Contribution(
      ContributionAction.EDIT_RELATION,
      annotationAfter.lastModifiedBy.get,
      annotationAfter.lastModifiedAt,
      Item(
        ItemType.RELATION,
        annotationAfter.annotates.documentId,
        document.getOwner,
        Some(annotationAfter.annotates.filepartId),
        Some(annotationAfter.annotates.contentType),
        Some(annotationAfter.annotationId),
        Some(annotationAfter.versionId),
        Some(relationBefore.bodies.map(_.value).mkString),
        Some(relationAfter.bodies.map(_.value).mkString)
      ),
      Seq.empty[String],
      getContext(annotationAfter)
    )
    
  private def deleteRelationContribution(annotationBefore: Annotation, annotationAfter: Annotation, deletedRelation: Relation, document: DocumentRecord) =
    Contribution(
      ContributionAction.DELETE_RELATION,
      annotationAfter.lastModifiedBy.get,
      annotationAfter.lastModifiedAt,
      Item(
        ItemType.RELATION,
        annotationAfter.annotates.documentId,
        document.getOwner,
        Some(annotationAfter.annotates.filepartId),
        Some(annotationAfter.annotates.contentType),
        Some(annotationAfter.annotationId),
        Some(annotationAfter.versionId),
        // At least currently, bodies have either value or URI - never both
        Some(deletedRelation.bodies.map(_.value).mkString),
        None
      ),
      Seq.empty[String],
      getContext(annotationAfter)
    )

  def validateUpdate(annotation: Annotation, previousVersion: Option[Annotation], document: DocumentRecord): Seq[Contribution] = {

    // TODO validation!
    // - make sure doc/filepart ID remains unchanged
    // - make sure filepart content type remains unchanged
    // - make sure annotation ID remains unchanged

    // TODO check any things the current user should not be able to manipulate
    // - createdAt/By info on bodies not touched by the user must be unchanged

    computeContributions(annotation, previousVersion, document)
  }
  
  /** Performs a 'diff' on the annotations, returning the corresponding Contributions **/
  def computeContributions(annotation: Annotation, previousVersion: Option[Annotation], document: DocumentRecord) =
    computeBodyContributions(annotation, previousVersion, document) ++
    computeRelationContributions(annotation, previousVersion, document)
    
  def computeBodyContributions(annotation: Annotation, previousVersion: Option[Annotation], document: DocumentRecord) = previousVersion match {
    case Some(before) =>
      // Body order never changes - so we compare before & after step by step
      annotation.bodies.foldLeft((Seq.empty[Contribution], before.bodies)) { case ((contributions, referenceBodies), nextBodyAfter) =>
        // Leading bodies that are not predecessors to bodies in the new annotation are DELETIONS
        val deletions = referenceBodies
          .takeWhile(bodyBefore => !isPredecessorTo(bodyBefore, nextBodyAfter))
          .map(deletedBody => deleteBodyContribution(before, annotation, deletedBody, document))

        // Once we're through detecting deletions, we continue with the remaining before-bodies
        val remainingReferenceBodies = referenceBodies.drop(deletions.size)
        if (remainingReferenceBodies.isEmpty) {
          // None left? Then this new body is an addition
          (contributions ++ deletions :+ createBodyContribution(annotation, nextBodyAfter, document),
            remainingReferenceBodies)
        } else if (remainingReferenceBodies.head.equalsIgnoreModified(nextBodyAfter)) {
          // This body is unchanged - continue with next
          (contributions ++ deletions,
            remainingReferenceBodies.tail)
        } else {
          // This body was updated
          (contributions ++ deletions :+ changeBodyContribution(before, annotation, remainingReferenceBodies.head, nextBodyAfter, document),
            remainingReferenceBodies.tail)
        }
      }._1 // We're not interested in the empty list of 'remaining reference bodies'

    case None =>
      if (annotation.lastModifiedBy.isEmpty)
        // We don't count 'contributions' made automatic processes
        // TODO new annotation with no previous version - generate contributions
        Seq.empty[Contribution]
      else
        annotation.bodies.map(body => createBodyContribution(annotation, body, document))
  }
    
  def computeRelationContributions(annotation: Annotation, previousVersion: Option[Annotation], document: DocumentRecord) = previousVersion match {
    case Some(before) =>
      // We'll keep this simple, with just delete/create
      val deleted = before.relations diff annotation.relations
      val created = annotation.relations diff before.relations
      
      // If adds >= deletes, well treat those as edits
      val edited = deleted.take(created.size).zip(created)
      
      val deleteContribs = deleted.drop(created.size).map(deleteRelationContribution(before, annotation, _, document))
      val createContribs = created.drop(deleted.size).map(createRelationContribution(annotation, _, document))
      val editContribs = edited.map { case (relationBefore, relationAfter) => 
        changeRelationContribution(before, annotation, relationBefore, relationAfter, document)
      }
      
      deleteContribs ++ editContribs ++ createContribs      
      
    case None =>
      // At the moment, this doesn't ever happen. Relations can only be added to existing annotations. But let's keep it, to be sure.
      annotation.relations.map(createRelationContribution(annotation, _, document))
  }

}
