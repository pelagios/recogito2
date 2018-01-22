package controllers.admin.gazetteers

import java.io.{InputStream, File, FileInputStream}
import java.util.zip.GZIPInputStream
import play.api.Logger
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import services.entity.EntityRecord
import services.entity.importer.EntityImporter

class DumpLoader {
  
  private def getStream(file: File, filename: String) =
    if (filename.endsWith(".gz"))
      new GZIPInputStream(new FileInputStream(file))
    else
      new FileInputStream(file)
  
  def importDump(file: File, filename: String, crosswalk: InputStream => Seq[EntityRecord], importer: EntityImporter)(implicit ctx: ExecutionContext) = {
    val records = crosswalk(getStream(file, filename))
    Logger.info("Importing " + records.size + " records")
    Await.result(importer.importRecords(records), 60.minute)   
  }
  
}