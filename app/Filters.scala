import javax.inject.Inject
import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter
import akka.stream.Materializer

class Filters @Inject() (implicit materializer: Materializer) extends HttpFilters {
  
  private val jsonGzipFilter = new GzipFilter(shouldGzip = (request, response) =>
    response.body.contentType.exists(_.startsWith("application/json")))
  
  def filters = Seq(jsonGzipFilter)
  
}