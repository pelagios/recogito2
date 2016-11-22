import javax.inject.Inject
import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter
import akka.stream.Materializer

class Filters @Inject() (implicit materializer: Materializer) extends HttpFilters {
  
  private val gzipFilter = new GzipFilter(shouldGzip = (request, response) =>
    response.body.contentType.exists { contentType => 
      contentType.startsWith("application/json") ||
      contentType.startsWith("text/csv") 
    })
  
  def filters = Seq(gzipFilter)
  
}