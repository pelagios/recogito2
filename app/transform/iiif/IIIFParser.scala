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
      
    case url if url.endsWith("manifest") || url.endsWith("manifest.json") => // Assuming presentation manifest as default
      Future.successful(Success(IIIF.MANIFEST))
      
    // Fetch and determine by inspection
    case url if url.endsWith("json") =>
      ws.url(url).withFollowRedirects(true).get().map { response =>
        (response.json \ "@type").asOpt[String] match {
          case Some(_) => Success(IIIF.MANIFEST) 
          case None => 
            if ((response.json \ "protocol").as[String].endsWith("api/image"))
              Success(IIIF.IMAGE_INFO)
            else
              Failure(new RuntimeException("Unsupported content type"))
        }
      } recover { case t: Throwable =>
        Failure(t)
      }
      
    case _ =>
      Future.successful(Failure(new RuntimeException("Unsupported content type")))
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
