package transform.iiif

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import transform.iiif.api.presentation.Manifest
import scala.io.Source
import play.api.libs.ws.WSClient
import play.api.libs.json.Json
import scala.util.{Failure, Success}

object IIIFParser {
  
  /** Identifies whether the given URI points to a manifest or image info.jso **/
  def identify(url: String)(implicit ws: WSClient, ctx: ExecutionContext): Future[Try[ResourceType]] = url match {
    // Not ideal...
    case url if url.endsWith("info.json") =>
      Future.successful(Success(IIIF.IMAGE_INFO))
      
    case url if url.endsWith("manifest") || url.endsWith("manifest.json") =>
      Future.successful(Success(IIIF.MANIFEST))
      
    // TODO fetch and determine by inspection
    // case url =>
      
    case _ =>
      Future.successful(Failure(new RuntimeException("Could not identify resource type")))
  }
  
  /** Fetches a manifset over the net and parses it **/
  def fetchManifest(manifestUrl: String)(implicit ws: WSClient, ctx: ExecutionContext): Future[Try[Manifest]] =
    ws.url(manifestUrl).withFollowRedirects(true).get().map { response =>
      Json.fromJson[Manifest](response.json) match {
        case s if s.isSuccess => 
          Success(s.get)
          
        case _ => 
          Failure(new RuntimeException(s"Failed to parse manifest ${manifestUrl}"))
      }
    }

}

sealed trait ResourceType

object IIIF {
  
  case object MANIFEST extends ResourceType
  case object IMAGE_INFO extends ResourceType
 
}
