package services.entity.importer

import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import services.entity._
import storage.ES

class EntityImporter (
  entityService    : EntityService,
  rewriter         : ReferenceRewriter,
  ENTITY_TYPE      : EntityType, 
  implicit val es  : ES,
  implicit val ctx : ExecutionContext
) extends HasBatchImport {
  
  /** Maximum number of URIs we will concatenate to an OR query **/
  private val MAX_URIS_IN_QUERY = 100
  
  /** If indexing takes longer than this, we'll throttle the import **/
  private val THROTTLING_THRESHOLD = 2000
  
  /** Throttling happens with a break of this value (ms) **/
  private val THROTTLE_DURATION = 5000
  
  /** Fetches all entities from the index that will be affected from adding this record **/
  private[entity] def getAffectedEntities(normalizedRecord: EntityRecord): Future[Seq[IndexedEntity]] = {
    // We need to query for this record's URI as well as all close/exactMatches
    val identifiers = normalizedRecord.uri +: normalizedRecord.directMatches

    // Protective measure - we don't really expect this to happen
    if (identifiers.size > MAX_URIS_IN_QUERY)
      throw new Exception(s"Maximum number of match URIs exceeded: ${identifiers.size} - ${identifiers.head}")

    entityService.findConnected(identifiers)
  }
  
  /** Takes a list of N records and conflates them to M entities, based on their link information **/ 
  private[entity] def conflateRecursive(records: Seq[EntityRecord], entities: Seq[Entity] = Seq.empty[Entity]): Seq[Entity] = {

    def conflateOne(r: EntityRecord, entities: Seq[Entity]): Seq[Entity] = {
      val connectedEntities = entities.filter(_.isConflationOf.exists(_.isConnectedWith(r)))
      val unconnectedItems = entities.diff(connectedEntities)
      EntityBuilder.join(r, connectedEntities, ENTITY_TYPE) +: unconnectedItems
    }

    if (records.isEmpty)
      entities // all done
    else
      conflateRecursive(
        records.tail, // Recurse with the tail
        conflateOne(records.head, entities) // process the head of the list
      )
  }
  
  /** Computes the reconflation of a new (normalized) record and the list of affected entities.
    * 
    * Uses 'conflateRecursive' (above) to do the actual work.
    */
  private def reconflate(normalizedRecord: EntityRecord, affectedEntities: Seq[IndexedEntity]) = {
    // Sorted affected entities by no. of records
    val sorted = affectedEntities.sortBy(- _.entity.isConflationOf.size)

    val records: Seq[EntityRecord] = {
      val affectedRecords = sorted.flatMap(_.entity.isConflationOf)

      // This record might update an existing one - find out where it is in the list
      val replaceIdx = affectedRecords.indexWhere(_.uri == normalizedRecord.uri)
      if (replaceIdx < 0)
        // Nope, the record is new - just append
        affectedRecords :+ normalizedRecord
      else
        // Replace the old version in the list
        affectedRecords.patch(replaceIdx, Seq(normalizedRecord), 1)
    }
    
    val conflatedItems: Seq[IndexedEntity] = {
      val entitiesAfterConflation = conflateRecursive(records)

      if (sorted.size > 0 && sorted.size != entitiesAfterConflation.size)
        Logger.info(s"Re-conflating ${sorted.size} entites to ${entitiesAfterConflation.size}")
      
        // In case multiple entities are merged, retain internal id
        if (sorted.size > 0 && entitiesAfterConflation.size == 1) {
          val oneBefore = sorted.head
          val after = entitiesAfterConflation.head
          Seq(IndexedEntity(
            after.copy(unionId = oneBefore.entity.unionId),
            oneBefore.version))
        } else {
          entitiesAfterConflation.map(e => IndexedEntity(e, None))
        }
    }

    // Pass back places before and after conflation
    (sorted, conflatedItems)
  }
  
  private def deleteMerged(entitiesBefore: Seq[IndexedEntity], entitiesAfter: Seq[IndexedEntity]): Future[Boolean] = {
    def idsBefore = entitiesBefore.map(_.entity.unionId).distinct
    def idsAfter = entitiesAfter.map(_.entity.unionId).distinct
    val toDelete = idsBefore diff idsAfter
    entityService.deleteEntities(toDelete)
  }
  
  /** Returns true only if the import has actually changed anything about the affected entities **/
  private def hasChanged(entitiesBefore: Seq[IndexedEntity], entitiesAfter: Seq[IndexedEntity]): Boolean =
    if (entitiesBefore.size != entitiesAfter.size) {
      // Different number of entities before and after - definitely changed
      true
    } else {
      // Same number - compare pairwise
      val pairwiseEquals = entitiesBefore.zip(entitiesAfter).map { case (before, after) =>
        before.entity equalsIgnoreLastSynced after.entity
      }
      pairwiseEquals.exists(_ == false)
    }
    
  /** Imports a single record into the index
    *
    * @return true if the import was successful, false otherwise  
    */
  override def importRecord(record: EntityRecord): Future[Boolean] = {
    val startTime = System.currentTimeMillis()
    val normalized = record.normalize

    // Reconflation step
    val fReconflate = getAffectedEntities(normalized).map { affected => reconflate(normalized, affected) }
    
    val f = for {
      (entitiesBefore, entitiesAfter) <- fReconflate
      upsertSuccess <- entityService.upsertEntities(entitiesAfter)
      deleteSuccess <- if (upsertSuccess) deleteMerged(entitiesBefore, entitiesAfter)
                       else Future.successful(false)
      rewriteSuccess <- if (deleteSuccess) {
                          if (hasChanged(entitiesBefore, entitiesAfter))
                            rewriter.rewriteReferencesTo(entitiesBefore, entitiesAfter)
                          else
                            Future.successful(true)
                        } else {
                          Future.successful(false)
                        }
    } yield (rewriteSuccess)
    
    f.map { success =>
      val took = System.currentTimeMillis - startTime
      if (took > THROTTLING_THRESHOLD) {
        Logger.warn(s"Indexing ${record.title} took ${took} ms - throttling")
        Thread.sleep(THROTTLE_DURATION)
      }
      success
    }
  }
  
}