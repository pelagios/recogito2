package services.contribution

import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import com.sksamuel.elastic4s.ElasticDsl._
import javax.inject.{Inject, Singleton}
import services.{HasDate, HasTryToEither, Page}
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.{JsSuccess, Json}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls
import scala.util.Try
import storage.es.ES
import services.HasTryToEither
import services.contribution.stats.ContributionStatsService

@Singleton
class ContributionService @Inject() (implicit val es: ES, val ctx: ExecutionContext) 
  extends HasDate with ContributionStatsService {

  implicit object ContributionIndexable extends Indexable[Contribution] {
    override def json(c: Contribution): String = Json.stringify(Json.toJson(c))
  }

  implicit object ContributionHitReader extends HitReader[(Contribution, String)] with HasTryToEither {
    override def read(hit: Hit): Either[Throwable, (Contribution, String)] =
      Try(Json.fromJson[Contribution](Json.parse(hit.sourceAsString)).get, hit.id)
  }

  /** Inserts a contribution record into the index **/
  def insertContribution(contribution: Contribution): Future[Boolean] =
    es.client execute {
      indexInto(ES.RECOGITO / ES.CONTRIBUTION) source contribution
    } map {
      _.created
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

  /** Retrieves a contribution by its ElasticSearch ID **/
  def findById(id: String): Future[Option[(Contribution, String)]] =
    es.client execute {
      get(id) from ES.RECOGITO / ES.CONTRIBUTION
    } map { response =>
      if (response.exists) {
        Json.fromJson[Contribution](Json.parse(response.sourceAsString)) match {
          case JsSuccess(contribution, _) =>
            Some(contribution, response.id)
          case _ =>
            // Should never happen
            throw new RuntimeException("Malformed contribution in index")
        }
      } else {
        None
      }
    }

  /** Returns the contribution history on a given document, as a paged result **/
  def getHistory(documentId: String, offset: Int = 0, limit: Int = 20): Future[Page[(Contribution, String)]] =
    es.client execute {
      search (ES.RECOGITO / ES.CONTRIBUTION) query (
        termQuery("affects_item.document_id" -> documentId)
      ) sortBy (
        fieldSort("made_at") order SortOrder.DESC
      ) start offset limit limit
    } map { response =>
      val contributionsWithId = response.to[(Contribution, String)].toSeq
      Page(response.tookInMillis, response.totalHits, offset, limit, contributionsWithId)
    }

  /** Shorthand to get the most recent contribution to the given document **/
  def getLastContribution(documentId: String) =
    getHistory(documentId, 0, 1).map(_.items.headOption.map(_._1))

  private def sortByField[B](docIds: Seq[String], sortOrder: services.SortOrder, offset: Int, limit: Int, field: Option[Contribution] => B)(implicit ordering: Ordering[B]) =
    Future.sequence(docIds.map(id => getLastContribution(id).map(contribution => (id, contribution)))).map { idsAndContributions =>
      val sorted = idsAndContributions.sortBy(t => field(t._2)).map(_._1).reverse
      if (sortOrder == services.SortOrder.ASC)
        sorted.drop(offset).take(limit)
      else
        sorted.dropRight(offset).takeRight(limit).reverse
    }

  /** Sorts the given list of document IDs by the time of last modification **/
  def sortDocsByLastModifiedAt(docIds: Seq[String], sortOrder: services.SortOrder, offset: Int, limit: Int) =
    sortByField(docIds, sortOrder, offset, limit, { c => c.map(_.madeAt.getMillis) })

  /** Sorts the given list of document IDs by the last modifying user **/
  def sortDocsByLastModifiedBy(docIds: Seq[String], sortOrder: services.SortOrder, offset: Int, limit: Int) =
    sortByField(docIds, sortOrder, offset, limit, { c => c.map(_.madeBy) })

  /** Deletes the contribution history after a given timestamp **/
  def deleteHistoryAfter(documentId: String, after: DateTime): Future[Boolean] = {

    def findContributionsAfter() = es.client execute {
      search (ES.RECOGITO / ES.CONTRIBUTION) query {
        boolQuery
          must (
            termQuery("affects_item.document_id" -> documentId)
          ) filter (
            rangeQuery("made_at").gt(formatDate(after))
          )
      } limit ES.MAX_SIZE
    } map { _.hits }

    findContributionsAfter().flatMap { hits =>
      if (hits.size > 0) {
        es.client.java.prepareBulk()
        es.client execute {
          bulk ( hits.map(h => delete(h.id) from ES.RECOGITO / ES.CONTRIBUTION) )
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
      search (ES.RECOGITO / ES.CONTRIBUTION) query {
        termQuery("affects_item.document_id" -> documentId)
      } limit ES.MAX_SIZE
    } map { _.hits }

    findContributions.flatMap { hits =>
      if (hits.size > 0) {
        es.client.java.prepareBulk()
        es.client execute {
          bulk ( hits.map(h => delete(h.id) from ES.RECOGITO / ES.CONTRIBUTION) )
        } map { response =>
          if (response.hasFailures)
            Logger.error("Failures while deleting contributions: " + response.failureMessage)
          !response.hasFailures
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

  /** Counts the contributions for the past 24 hours **/
  def countLast24hrs() =
    es.client execute {
      search (ES.RECOGITO / ES.CONTRIBUTION) query {
        constantScoreQuery {
          rangeQuery("made_at").gt("now-24h")
        }
      } size 0
    } map { _.totalHits }

  /** Gets the most recent N contributions **/
  def getMostRecent(n: Int): Future[Seq[Contribution]] =
    es.client execute {
      search(ES.RECOGITO / ES.CONTRIBUTION) sortBy (
        fieldSort("made_at") order SortOrder.DESC
      ) limit n
    } map { response =>
      response.to[(Contribution, String)].toSeq.map(_._1)
    }

}
