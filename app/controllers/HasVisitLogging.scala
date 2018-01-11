package controllers

import eu.bitwalker.useragentutils.UserAgent
import services.visit._
import services.ContentType
import services.document.DocumentAccessLevel
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import org.joda.time.DateTime
import play.api.mvc.{AnyContent, RequestHeader}
import play.api.http.HeaderNames
import scala.concurrent.Future

trait HasVisitLogging {
    
  /** Common code for logging a visit **/
  private def log(
      doc: Option[DocumentRecord],
      part: Option[DocumentFilepartRecord],
      responseFormat: String,
      accesslevel: Option[DocumentAccessLevel]
    )(implicit request: RequestHeader, visitService: VisitService): Future[Unit] = {
    
    val userAgentHeader = request.headers.get(HeaderNames.USER_AGENT)
    val userAgent = userAgentHeader.map(ua => UserAgent.parseUserAgentString(ua))
    val os = userAgent.map(_.getOperatingSystem)
    
    val item = doc.map(doc => VisitedItem(
      doc.getId,
      doc.getOwner,
      part.map(_.getId),
      part.flatMap(pt => ContentType.withName(pt.getContentType))
    ))
    
    val visit = Visit(
      request.uri,
      request.headers.get(HeaderNames.REFERER),
      DateTime.now(),
      Client(
        request.remoteAddress,
        userAgentHeader.getOrElse("UNKNOWN"),
        userAgent.map(_.getBrowser.getGroup.getName).getOrElse("UNKNOWN"),
        os.map(_.getName).getOrElse("UNKNOWN"),
        os.map(_.getDeviceType.getName).getOrElse("UNKNOWN")  
      ),
      responseFormat,
      item,
      accesslevel)
    
    if (HasVisitLogging.isBot(visit))
      Future.successful(())
    else
      visitService.insertVisit(visit)    
  }
  
  def logPageView()(implicit request: RequestHeader, visitService: VisitService) =
    log(None, None, "text/html", None)

  def logDocumentView(
      doc: DocumentRecord, part: Option[DocumentFilepartRecord], accesslevel: DocumentAccessLevel
    )(implicit request: RequestHeader, visitService: VisitService) = 
      log(Some(doc), part, "text/html", Some(accesslevel))
    
  def logDownload(
      doc: DocumentRecord, responseFormat: String
    )(implicit request: RequestHeader, visitService: VisitService) =
      log(Some(doc), None, responseFormat, None)
     
    
}

object HasVisitLogging {
  
  // If one of these keywords appears in the UA header, treat as bot
  private val USER_AGENT_EXCLUDES = Set("uptimerobot")
   
  def isBot(visit: Visit) =
    USER_AGENT_EXCLUDES.find(visit.client.userAgent.contains(_)).isDefined
  
}