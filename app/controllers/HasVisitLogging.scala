package controllers

import eu.bitwalker.useragentutils.UserAgent
import models.visit._
import models.document.DocumentAccessLevel
import models.generated.tables.records.DocumentRecord
import play.api.mvc.{ AnyContent, RequestHeader }
import play.api.http.HeaderNames
import org.joda.time.DateTime
import models.generated.tables.records.DocumentFilepartRecord
import models.ContentType

trait HasVisitLogging {
  
  /** Common code for logging a visit **/
  private def log(
      doc: Option[DocumentRecord],
      part: Option[DocumentFilepartRecord],
      responseFormat: String,
      accesslevel: Option[DocumentAccessLevel]
    )(implicit request: RequestHeader, visitService: VisitService) = {
    
    val userAgentHeader = request.headers.get(HeaderNames.USER_AGENT)
    val userAgent = userAgentHeader.map(ua => UserAgent.parseUserAgentString(ua))
    val os = userAgent.map(_.getOperatingSystem)
    
    val item = doc.map(doc => VisitedItem(
      doc.getId,
      doc.getOwner,
      part.map(_.getId),
      part.flatMap(pt => ContentType.withName(pt.getContentType))
    ))
    
    visitService.insertVisit(Visit(
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
      accesslevel
    ))    
  }
  
  def logPageView(accesslevel: Option[DocumentAccessLevel])(implicit request: RequestHeader, visitService: VisitService) =
    log(None, None, "text/html", accesslevel)

  def logDocumentView(
      doc: DocumentRecord, part: Option[DocumentFilepartRecord], accesslevel: DocumentAccessLevel
    )(implicit request: RequestHeader, visitService: VisitService) = 
      log(Some(doc), part, "text/html", Some(accesslevel))
    
  def logDownload(
      doc: DocumentRecord, responseFormat: String
    )(implicit request: RequestHeader, visitService: VisitService) =
      log(Some(doc), None, responseFormat, None)
     
    
}