package models.contribution

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import storage.ES

object  ContributionService {
  
  private val CONTRIBUTION = "contribution"
  
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
  
}