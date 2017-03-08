package models.visit

import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import storage.ES

@Singleton
class VisitService @Inject() (implicit val es: ES, val ctx: ExecutionContext) {
  
  implicit object VisitIndexable extends Indexable[Visit] {
    override def json(v: Visit): String = Json.stringify(Json.toJson(v))
  }

  implicit object VisitHitAs extends HitAs[Visit] {
    override def as(hit: RichSearchHit): Visit =
      Json.fromJson[Visit](Json.parse(hit.sourceAsString)).get
  }
  
  def insertVisit(visit: Visit): Future[Unit] =
    es.client execute {
      index into ES.RECOGITO / ES.VISIT source visit
    } map { _ => 
    } recover { case t: Throwable =>
      Logger.error("Error logging visit event")
      val foo = t.printStackTrace
    }
    
  def countTotal(): Future[Long] =
    es.client execute {
      search in ES.RECOGITO / ES.VISIT limit 0
    } map { _.totalHits }
  
}