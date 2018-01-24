package services.visit

import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import com.sksamuel.elastic4s.searches.{RichSearchResponse, RichSearchHit}
import com.sksamuel.elastic4s.ElasticDsl._
import java.nio.file.Path
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.libs.Files.{TemporaryFile, TemporaryFileCreator}
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try
import services.{HasDate, HasTryToEither}
import storage.es.ES
import org.elasticsearch.index.reindex.DeleteByQueryAction
import storage.es.HasScrollProcessing

@Singleton
class VisitService @Inject() (implicit val es: ES, val ctx: ExecutionContext) extends HasScrollProcessing with HasDate {
 
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
      search(ES.RECOGITO / ES.VISIT) limit 0
    } map { _.totalHits }
    
  def countSince(date: DateTime): Future[Long] =
    es.client execute {
      search(ES.RECOGITO / ES.VISIT) query {
        rangeQuery("visited_at").gt(formatDate(date))
      } limit 0
    } map { _.totalHits }
    
  def scrollExport()(implicit creator: TemporaryFileCreator): Future[Path] = {
    val exporter = CsvExporter.createNew()
    
    def writeToFile(response: RichSearchResponse): Future[Boolean] = 
      Future {
        val visits = response.to[Visit]
        exporter.writeBatch(visits)
      } map { _ => true 
      } recover { case t: Throwable =>
        t.printStackTrace()
        false
      }

    es.client execute {
      search(ES.RECOGITO / ES.VISIT) query matchAllQuery limit 200 scroll "5m"
    } flatMap { scroll(writeToFile, _) } map { success =>
      exporter.close()
      if (success) exporter.path else throw new RuntimeException()
    } recover { case t: Throwable =>
      Try(exporter.close())
      throw t
    }
  }
  
  def deleteOlderThan(date: DateTime): Future[Boolean] =
    es.client execute {
      deleteIn(ES.RECOGITO / ES.VISIT) by {
        rangeQuery("visited_at").lt(formatDate(date))
      }
    } map { _ => true
    } recover { case t: Throwable =>
      t.printStackTrace()
      false
    }
  
}