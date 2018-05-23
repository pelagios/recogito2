package services.annotation.relation

import com.sksamuel.elastic4s.ElasticDsl._
import java.util.UUID
import scala.concurrent.Future
import services.annotation.{Annotation, AnnotationService}
import storage.es.ES

trait RelationService { self: AnnotationService =>
  
  /** Fetch all annotations that have relations.
    *  
    * Works as a two-step process. First, we grab all source annotations using an 
    * existsQuery. Then, we inspect the Ids of all destination annotations, and fetch 
    * those that are missing. (I.e. those that are not source annotations themselves.)
    */
  def findWithRelationByDocId(id: String, offset: Int = 0, limit: Int = ES.MAX_SIZE): Future[Seq[Annotation]] = {
    // Fetches source annotations from the index, i.e. those that have a 'relations' field
    val fSourceAnnotations = es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION) query {
        boolQuery
          must (
            termQuery("annotates.document_id" -> id),
            existsQuery("relations.relates_to")
          )
      }
    } map { _.to[(Annotation, Long)].toSeq.map(_._1) }

    // Takes a list of source annotations, inspects destination Ids, and returns
    // the Ids of annotations that are not yet included in the source annotations list
    def determineMissingDestinations(sourceAnnotations: Seq[Annotation]) = {
      val sourceIds = sourceAnnotations.map(_.annotationId)
      val destinationIds = sourceAnnotations.flatMap(_.relations.map(_.relatesTo)).distinct
      destinationIds diff sourceIds
    }

    // Batch-retrieves a list of annotations by their Ids
    def findByIds(ids: Seq[UUID]) = es.client execute {
      multiget (
        ids.map { id => get(id.toString) from ES.RECOGITO / ES.ANNOTATION }
      )
    } map { _.to[(Annotation, Long)].toSeq.map(_._1) }
    
    for {
      sourceAnnotations <- fSourceAnnotations
      additionalDestinations <- findByIds(determineMissingDestinations(sourceAnnotations))
    } yield (sourceAnnotations ++ additionalDestinations)
  }
  
}