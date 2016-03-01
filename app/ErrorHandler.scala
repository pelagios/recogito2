import javax.inject.{ Inject, Provider }
import play.api.{ Configuration, Environment, Logger, OptionalSourceMapper, UsefulException }
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import play.api.routing.Router
import scala.concurrent.Future

class ErrorHandler @Inject() (
    env: Environment,
    config: Configuration,
    sourceMapper: OptionalSourceMapper,
    router: Provider[Router]
  ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  override def onProdServerError(request: RequestHeader, exception: UsefulException) =
    Future.successful(InternalServerError("A server error occurred: " + exception.getMessage))

  override def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    if (statusCode == 413) // Request entity too large
      Future.successful(EntityTooLarge("Could not upload the file - too large"))
    else 
      super.onClientError(request, statusCode, message)
  }
  
}