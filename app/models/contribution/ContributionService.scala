package models.contribution

import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import javax.inject.{ Inject, Singleton }
import models.{ HasDate, Page }
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.aggregations.bucket.nested.Nested
import org.elasticsearch.search.aggregations.bucket.histogram.{ DateHistogramInterval, InternalHistogram }
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.Logger
import play.api.libs.json.{ JsSuccess, Json }
import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.reflectiveCalls
import storage.ES

@Singleton
class ContributionService @Inject() (implicit val es: ES, val ctx: ExecutionContext) extends HasDate {

  implicit object ContributionIndexable extends Indexable[Contribution] {
    override def json(c: Contribution): String = Json.stringify(Json.toJson(c))
  }

  implicit object ContributionHitAs extends HitAs[(Contribution, String)] {
    override def as(hit: RichSearchHit): (Contribution, String) =
      (Json.fromJson[Contribution](Json.parse(hit.sourceAsString)).get, hit.id)
  }

  /** Inserts a contribution record into the index **/
  def insertContribution(contribution: Contribution): Future[Boolean] =
    es.client execute {
      index into ES.RECOGITO / ES.CONTRIBUTION source contribution
    } map {
      _.isCreated
    } recover { case t: Throwable =>
      Logger.error("Error recording contribution event")
      Logger.error(contribution.toString)
      t.printStackTrace
      false
    }

  /** Inserts a list of contributions, automatically dealing with retries. **/
  def insertContributions(contributions: Seq[Contribution], retries: Int = ES.MAX_RETRIES): Future[Boolean] =
    contributions.foldLeft(Future.successful(Seq.empty[Contribution])) { case (future, contribution) =>
      future.flatMap { failed =>
        insertContribution(contribution).map { success =>
          if (success) failed else failed :+ contribution
        }
      }
    } flatMap { failed =>
      if (failed.size > 0 && retries > 0) {
        Logger.warn(failed.size + " annotations failed to import - retrying")
        insertContributions(failed, retries - 1)
      } else {
        if (failed.size > 0) {
          Logger.error(failed.size + " contribution events failed without recovery")
          Future.successful(false)
        } else {
          Future.successful(true)
        }
      }
    }
    
  def getMostRecent(n: Int): Future[Seq[Contribution]] =
    es.client execute {
      search in ES.RECOGITO / ES.CONTRIBUTION sort (
        field sort "made_at" order SortOrder.DESC
      ) limit n
    } map { response =>
      response.as[(Contribution, String)].toSeq.map(_._1)
    }

  /** Returns the contribution history on a given document, as a paged result **/
  def getHistory(documentId: String, offset: Int = 0, limit: Int = 20): Future[Page[(Contribution, String)]] =
    es.client execute {
      search in ES.RECOGITO / ES.CONTRIBUTION query nestedQuery("affects_item").query (
        termQuery("affects_item.document_id" -> documentId)
      ) sort (
        field sort "made_at" order SortOrder.DESC
      ) start offset limit limit
    } map { response =>
      val contributionsWithId = response.as[(Contribution, String)].toSeq
      Page(response.getTook.getMillis, response.getHits.getTotalHits, offset, limit, contributionsWithId)
    }
    
  /** Shorthand to get the most recent contribution to the given document **/
  def getLastContribution(documentId: String) =
    getHistory(documentId, 0, 1).map(_.items.headOption.map(_._1))
    
  private def sortByField[B](docIds: Seq[String], sortOrder: models.SortOrder, offset: Int, limit: Int, field: Option[Contribution] => B)(implicit ordering: Ordering[B]) =
    Future.sequence(docIds.map(id => getLastContribution(id).map(contribution => (id, contribution)))).map { idsAndContributions => 
      val sorted = idsAndContributions.sortBy(t => field(t._2)).map(_._1).reverse
      if (sortOrder == models.SortOrder.ASC)
        sorted.drop(offset).take(limit)
      else
        sorted.dropRight(offset).takeRight(limit).reverse
    }    
      
  /** Sorts the given list of document IDs by the time of last modification **/
  def sortDocsByLastModifiedAt(docIds: Seq[String], sortOrder: models.SortOrder, offset: Int, limit: Int) =
    sortByField(docIds, sortOrder, offset, limit, { c => c.map(_.madeAt.getMillis) })
    
  /** Sorts the given list of document IDs by the last modifying user **/
  def sortDocsByLastModifiedBy(docIds: Seq[String], sortOrder: models.SortOrder, offset: Int, limit: Int) =
    sortByField(docIds, sortOrder, offset, limit, { c => c.map(_.madeBy) })
  
  /** Retrieves a contribution by its ElasticSearch ID **/
  def findById(id: String): Future[Option[(Contribution, String)]] =
    es.client execute {
      get id id from ES.RECOGITO / ES.CONTRIBUTION
    } map { response =>
      if (response.isExists) {
        Json.fromJson[Contribution](Json.parse(response.sourceAsString)) match {
          case JsSuccess(contribution, _) =>
            Some(contribution, response.getId)
          case _ =>
            // Should never happen
            throw new RuntimeException("Malformed contribution in index")
        }
      } else {
        None
      }
    }

  /** Deletes the contribution history after a given timestamp **/
  def deleteHistoryAfter(documentId: String, after: DateTime): Future[Boolean] = {
    
    def findContributionsAfter() = es.client execute {
      search in ES.RECOGITO / ES.CONTRIBUTION query {
        bool {
          must (
            nestedQuery("affects_item").query(termQuery("affects_item.document_id" -> documentId))
          ) filter (
            rangeQuery("made_at").from(formatDate(after)).includeLower(false)
          )
        }
      } limit ES.MAX_SIZE
    } map { _.getHits.getHits }

    findContributionsAfter().flatMap { hits =>
      if (hits.size > 0) {
        es.client execute {
          bulk ( hits.map(h => delete id h.getId from ES.RECOGITO / ES.CONTRIBUTION) )
        } map {
          !_.hasFailures
        } recover { case t: Throwable =>
          t.printStackTrace()
          false
        }
      } else {
        // Nothing to delete
        Future.successful(true)
      }
    }
  }
  
  /** Deletes the complete contribution history.
    *
    * Note that this method will leave the annotation history index untouched. I.e.
    * for a "client-side" delete of the history, you need to make sure that you also
    * delete the annotations and annotation versions.
    */
  def deleteHistory(documentId: String): Future[Boolean] = {
    
    def findContributions() = es.client execute {
      search in ES.RECOGITO / ES.CONTRIBUTION query {
        nestedQuery("affects_item").query(termQuery("affects_item.document_id" -> documentId))
      } limit ES.MAX_SIZE
    } map { _.getHits.getHits }
    
    findContributions.flatMap { hits =>
      if (hits.size > 0) {
        es.client execute {
          bulk ( hits.map(h => delete id h.getId from ES.RECOGITO / ES.CONTRIBUTION) )
        } map {
          !_.hasFailures
        } recover { case t: Throwable =>
          t.printStackTrace()
          false
        }       
      } else {
        // Nothing to delete
        Future.successful(true)
      }
    }
  }

  /** Returns the system-wide contribution stats **/
  def getGlobalStats(): Future[ContributionStats] =
    es.client execute {
      search in ES.RECOGITO / ES.CONTRIBUTION aggs (
        aggregation terms "by_user" field "made_by",
        aggregation terms "by_action" field "action",
        aggregation nested("by_item_type") path "affects_item" aggs (
          aggregation terms "item_type" field "affects_item.item_type"
        ),
        aggregation filter "contribution_history" filter (rangeQuery("made_at") from "now-30d") aggs (
           aggregation datehistogram "last_30_days" field "made_at" minDocCount 0 interval DateHistogramInterval.DAY
        )
      ) limit 0
    } map { response =>
      val byUser = response.aggregations.get("by_user").asInstanceOf[Terms]
      val byAction = response.aggregations.get("by_action").asInstanceOf[Terms]
      val byItemType = response.aggregations.get("by_item_type").asInstanceOf[Nested]
        .getAggregations.get("item_type").asInstanceOf[Terms]
      
      val contributionHistory = response.aggregations.get("contribution_history").asInstanceOf[InternalFilter]
        .getAggregations.get("last_30_days").asInstanceOf[InternalHistogram[InternalHistogram.Bucket]]
        
      ContributionStats(
        response.getTookInMillis,
        response.getHits.getTotalHits,
        byUser.getBuckets.asScala.map(bucket =>
          (bucket.getKeyAsString, bucket.getDocCount)),
        byAction.getBuckets.asScala.map(bucket =>
          (ContributionAction.withName(bucket.getKeyAsString), bucket.getDocCount)),
        byItemType.getBuckets.asScala.map(bucket =>
          (ItemType.withName(bucket.getKeyAsString), bucket.getDocCount)),
        contributionHistory.getBuckets.asScala.map(bucket =>
          (new DateTime(bucket.getKey.asInstanceOf[DateTime].getMillis, DateTimeZone.UTC), bucket.getDocCount)))
    }

}
