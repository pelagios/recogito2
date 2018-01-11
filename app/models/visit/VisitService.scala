package models.visit

import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import com.sksamuel.elastic4s.ElasticDsl._
import javax.inject.{ Inject, Singleton }
import models.HasTryToEither
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import scala.util.Try
import storage.ES
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError

@Singleton
class VisitService @Inject() (implicit val es: ES, val ctx: ExecutionContext) {
 
  implicit object VisitIndexable extends Indexable[Visit] {
    override def json(v: Visit): String = Json.stringify(Json.toJson(v))
  }

  implicit object VisitHitReader extends HitReader[Visit] with HasTryToEither {
    override def read(hit: Hit): Either[Throwable, Visit] =
      Try(Json.fromJson[Visit](Json.parse(hit.sourceAsString)).get)
  }
  
  def insertVisit(visit: Visit): Future[Unit] =
    es.client execute {
      indexInto(ES.RECOGITO / ES.VISIT).doc(visit)
    } map { _ =>
    } recover { case t: Throwable =>
      t.printStackTrace
    }
    
  def countTotal(): Future[Long] =
    es.client execute {
      search in ES.RECOGITO / ES.VISIT limit 0
    } map { _.totalHits }
  
}