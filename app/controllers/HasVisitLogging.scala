package controllers

import eu.bitwalker.useragentutils.UserAgent
import models.visit._
import models.generated.tables.records.DocumentRecord
import play.api.mvc.{ AnyContent, Request }
import play.api.http.HeaderNames
import org.joda.time.DateTime
import models.generated.tables.records.DocumentFilepartRecord
import models.ContentType

trait HasVisitLogging {
  
  def logVisit(doc: DocumentRecord, part: Option[DocumentFilepartRecord], responseFormat: String)(implicit request: Request[AnyContent]) = {
    val userAgentString = request.headers.get(HeaderNames.USER_AGENT)
    val userAgent = userAgentString.map(ua => UserAgent.parseUserAgentString(ua))
    val os = userAgent.map(_.getOperatingSystem)
    
    val visit = Visit(
      request.uri,
      request.headers.get(HeaderNames.REFERER),
      DateTime.now(),
      Client(
        request.remoteAddress,
        userAgentString.getOrElse("UNKNOWN"),
        userAgent.map(_.getBrowser.getGroup.getName).getOrElse("UNKNOWN"),
        os.map(_.getName).getOrElse("UNKNOWN"),
        os.map(_.getDeviceType.getName).getOrElse("UNKNOWN")  
      ),
      responseFormat,
      Some(VisitedItem(
        doc.getId,
        doc.getOwner,
        part.map(_.getId),
        part.flatMap(p => ContentType.withName(p.getContentType))
      ))
    )
   
  }
  
}