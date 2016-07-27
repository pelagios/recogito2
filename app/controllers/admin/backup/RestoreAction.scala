package controllers.admin.backup

import collection.JavaConverters._
import java.io.File
import java.util.UUID
import java.sql.Timestamp
import java.util.zip.{ ZipEntry, ZipFile }
import models.HasDate
import models.annotation.{ Annotation, AnnotationService }
import models.document.DocumentService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.joda.time.DateTime
import play.api.libs.json._
import scala.concurrent.ExecutionContext
import scala.io.Source
import storage.{ DB, ES }

trait RestoreAction extends HasDate {

  private def parseDocumentMetadata(json: JsValue) =    
    new DocumentRecord(
      (json \ "id").as[String],
      (json \ "owner").as[String],
      new Timestamp((json \ "uploaded_at").as[DateTime].getMillis),
      (json \ "title").as[String],
      (json \ "author").asOpt[String].getOrElse(null),
      null, // TODO date_numeric
      (json \ "date_freeform").asOpt[String].getOrElse(null),
      (json \ "description").asOpt[String].getOrElse(null),
      (json \ "language").asOpt[String].getOrElse(null),
      (json \ "source").asOpt[String].getOrElse(null),
      (json \ "edition").asOpt[String].getOrElse(null),
      (json \ "is_public").as[Boolean])
    
  private def parseFilepartMetadata(documentId: String, json: JsValue) =
    (json \ "parts").as[Seq[JsObject]].zipWithIndex.map { case (obj, idx) =>
      new DocumentFilepartRecord(
        (obj \ "id").as[UUID],
        documentId,
        (obj \ "title").as[String],
        (obj \ "content_type").as[String],
        (obj \ "filename").as[String],
        idx + 1)
    }
  
  private def parseAnnotations(lines: Iterator[String]) =
    lines.map(line => Json.fromJson[Annotation](Json.parse(line)).get)
 
  def restoreFromZip(file: File, annotationService: AnnotationService)(implicit es: ES, db: DB, context: ExecutionContext) = {
    val zipFile = new ZipFile(file)
    val entries = zipFile.entries.asScala.toSeq.filter(!_.getName.startsWith("__MACOSX")) // Damn you Apple!
    
    val metadataEntry = entries.filter(_.getName == "metadata.json").head
    val metadataJson  = Json.parse(Source.fromInputStream(zipFile.getInputStream(metadataEntry), "UTF-8").getLines.mkString("\n"))
    
    val documentRecord  = parseDocumentMetadata(metadataJson)
    val filepartRecords = parseFilepartMetadata(documentRecord.getId, metadataJson) 
    
    val fileparts = filepartRecords.map { record =>
      val entry = entries.filter(_.getName == "parts" + File.separator + record.getFilename).head
      val inputstream = zipFile.getInputStream(entry) 
      (record, inputstream)
    }
    
    val annotationEntry = entries.filter(_.getName == "annotations.jsonl").head
    val annotationLines = Source.fromInputStream(zipFile.getInputStream(annotationEntry), "UTF-8").getLines
    val annotations = parseAnnotations(annotationLines).toSeq

    for {
     _ <- DocumentService.importDocument(documentRecord, fileparts)
     _ <- annotationService.insertOrUpdateAnnotations(annotations)
    } yield Unit

  }
  
}