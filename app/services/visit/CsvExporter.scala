package services.visit

import java.nio.file.Paths
import java.util.UUID
import kantan.csv.{CsvConfiguration, CsvWriter}
import kantan.csv.CsvConfiguration.{Header, QuotePolicy}
import kantan.csv.ops._
import kantan.csv.engine.commons._
import play.api.libs.Files.{TemporaryFile, TemporaryFileCreator}
import services.HasDate
import storage.TempDir

class CsvExporter private (private val tmp: TemporaryFile, val writer: CsvWriter[Seq[String]]) extends HasDate {
  
  val path = tmp.path
  
  private def toRow(v: Visit): Seq[String] = 
    Seq(
      v.url,
      v.referer.getOrElse(""),
      formatDate(v.visitedAt),
      v.client.ip,
      v.client.userAgent,
      v.client.browser,
      v.client.os,
      v.client.deviceType,
      v.responseFormat,
      v.visitedItem.map(_.documentId).getOrElse(""),
      v.visitedItem.map(_.documentOwner).getOrElse(""),
      v.visitedItem.flatMap(_.filepartId.map(_.toString)).getOrElse(""),
      v.visitedItem.flatMap(_.contentType.map(_.toString)).getOrElse(""),
      v.accessLevel.map(_.toString).getOrElse(""))
  
  def writeBatch(visits: Seq[Visit]) = 
    visits.foreach { v => writer.write(toRow(v)) }
  
  def close() = writer.close()
  
}

object CsvExporter {
  
 def createNew()(implicit creator: TemporaryFileCreator, conf: play.api.Configuration): CsvExporter = {
    val tmpDir = TempDir.get()(conf)
    val header = Seq(
      "URL", 
      "REFERER", 
      "VISITED_AT", 
      "CLIENT_IP",
      "CLIENT_USER_AGENT",
      "CLIENT_BROWSER",
      "CLIENT_OS",
      "CLIENT_DEVICE",
      "RESPONSE_FORMAT",
      "VISITED_DOC",
      "VISITED_DOC_OWNER",
      "VISITED_DOC_PART",
      "VISITED_DOC_CONTENT_TYPE",
      "ACCESS_LEVEL")

    val tmp = creator.create(Paths.get(tmpDir, s"${UUID.randomUUID}.csv"))
    val file = tmp.path.toFile
    val config = CsvConfiguration(',', '"', QuotePolicy.Always, Header.Explicit(header))
    val writer = file.asCsvWriter[Seq[String]](config)
    
    new CsvExporter(tmp, writer)
  }
  
}

