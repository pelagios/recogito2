package services.visit

import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import com.sksamuel.elastic4s.searches.{RichSearchResponse, RichSearchHit}
import com.sksamuel.elastic4s.ElasticDsl._
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.libs.Files.{TemporaryFile, TemporaryFileCreator}
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try
import services.HasTryToEither
import storage.ES
import java.nio.file.Path

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
      search(ES.RECOGITO / ES.VISIT) limit 0
    } map { _.totalHits }
    
  def countSince(date: DateTime): Future[Long] = ???
    
  def scrollExport()(implicit creator: TemporaryFileCreator): Future[Path] = {
    
    val exporter = CsvExporter.createNew()
    
    def fetchNextBatch(scrollId: String): Future[RichSearchResponse] =
      es.client execute { searchScroll(scrollId) keepAlive "1m" }
    
    def writeToFile(hits: Seq[Visit]): Future[Boolean] = Future {
      exporter.writeBatch(hits)
    } map { _ => true 
    } recover { case t: Throwable =>
      t.printStackTrace()
      false
    }
    
    def exportBatch(response: RichSearchResponse, cursor: Long = 0l): Future[Boolean] = {
      if (response.hits.isEmpty) {
        Future.successful(true)
      } else {
        writeToFile(response.to[Visit]).flatMap { success =>
          val writtenRecords = cursor + response.hits.size
          if (writtenRecords < response.totalHits)
            fetchNextBatch(response.scrollId).flatMap(exportBatch(_, writtenRecords).map(_ && success))
          else
            Future.successful(success)
        }
      }
    }
    
    es.client execute {
      search(ES.RECOGITO / ES.VISIT) query matchAllQuery limit 200 scroll "1m"
    } flatMap { exportBatch(_) } map { success =>
      exporter.close()
      if (success) exporter.path else throw new RuntimeException()
    } recover { case t: Throwable =>
      Try(exporter.close())
      throw t
    }
   
  }
  
}