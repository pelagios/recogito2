package models.contribution

import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import storage.ES

object  ContributionService {
  
  private val CONTRIBUTION = "contribution"
 
  // Maximum number of times an annotation (batch) will be retried in case of failure 
  private def MAX_RETRIES = 10
  
  implicit object ContributionIndexable extends Indexable[Contribution] {
    override def json(c: Contribution): String = Json.stringify(Json.toJson(c))
  }

  implicit object ContributionHitAs extends HitAs[Contribution] {
    override def as(hit: RichSearchHit): Contribution =
      Json.fromJson[Contribution](Json.parse(hit.sourceAsString)).get
  }
  
  def insertContribution(contribution: Contribution)(implicit context: ExecutionContext): Future[Boolean] =
    ES.client execute {      
      index into ES.IDX_RECOGITO / CONTRIBUTION source contribution
    } map {
      _.isCreated
    } recover { case t: Throwable =>
      Logger.error("Error recording contribution event")
      Logger.error(contribution.toString)
      t.printStackTrace
      false
    }
        
  def insertContributions(contributions: Seq[Contribution], retries: Int = MAX_RETRIES)(implicit context: ExecutionContext): Future[Boolean] =
    contributions.foldLeft(Future.successful(Seq.empty[Contribution])) { case (future, contribution) =>
      future.flatMap { failed =>
        insertContribution(contribution).map { success =>
          if (success)
            failed
          else
            failed :+ contribution
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
  
}