package storage.es

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.{RichSearchResponse, RichSearchHit, SearchDefinition}
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.mutable.ListBuffer

trait HasScrollProcessing {

  private def fetchNextBatch(scrollId: String)(implicit es: ES, ctx: ExecutionContext) = {
    es.client execute { searchScroll(scrollId) keepAlive "5m" }
  }
  
  def scroll(
    fn: RichSearchResponse => Future[Boolean],
    response: RichSearchResponse,
    cursor: Long = 0l
  )(implicit es: ES, ctx: ExecutionContext): Future[Boolean] = 
    
    if (response.hits.isEmpty) {
      Future.successful(true)
    } else {
      fn(response).flatMap { success =>
        val processed = cursor + response.hits.size
        if (processed < response.totalHits)
          fetchNextBatch(response.scrollId).flatMap { response =>
            scroll(fn, response, processed).map(_ && success)
          }
        else
          Future.successful(success)
      }
    }
  
  def scrollQuery(q: SearchDefinition)(implicit es: ES, ctx: ExecutionContext) = {
    
    def append(response: RichSearchResponse, previous: Seq[RichSearchHit] = Seq.empty[RichSearchHit]): Future[Seq[RichSearchHit]] = {
      val hits = response.hits.toSeq
      val hasMore = hits.size + previous.size < response.totalHits
      if (hasMore)
        fetchNextBatch(response.scrollId).flatMap { response =>
          append(response, previous ++ hits)
        }
      else
        Future.successful(previous ++ hits)
    }
   
    es.client execute { 
      q limit 1000 scroll "5m" 
    } flatMap { append(_) }
    
  }
  
}
