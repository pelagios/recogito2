package storage.es

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.RichSearchResponse
import scala.concurrent.{ExecutionContext, Future}

trait HasScrollProcessing {

  private def fetchNextBatch(scrollId: String)(implicit es: ES, ctx: ExecutionContext) =
    es.client execute { searchScroll(scrollId) keepAlive "5m" }

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

}
