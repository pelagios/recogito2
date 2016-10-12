package controllers.admin.backup

import collection.JavaConverters._
import java.io.{ File, FileInputStream }
import java.util.UUID
import java.sql.Timestamp
import java.util.zip.{ ZipEntry, ZipFile }
import models.{ ContentType, HasContentTypeList, HasDate }
import models.annotation._
import models.document.DocumentService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext
import scala.io.Source
import storage.{ DB, ES }

trait RestoreAction extends HasDate {
  
  private case class AnnotatedObjectStub(
    documentId: Option[String],
    filepartId: Option[UUID],
    filepartTitle: Option[String],
    contentType: ContentType
  ) {
    
    def toAnnotatedObject(docId: String, fileparts: Seq[DocumentFilepartRecord]) = AnnotatedObject(
      documentId.getOrElse(docId),
      filepartId.getOrElse(fileparts.find(_.getTitle.equals(filepartTitle.get)).get.getId),
      contentType: ContentType
    )
    
  }
  
  private object AnnotatedObjectStub extends HasContentTypeList {
  
    implicit val annotatedObjectStubReads: Reads[AnnotatedObjectStub] = (
      (JsPath \ "document_id").readNullable[String] and
      (JsPath \ "filepart_id").readNullable[UUID] and
      (JsPath \ "filepart_title").readNullable[String] and
      (JsPath \ "content_type").read[JsValue].map(fromCTypeList)
    )(AnnotatedObjectStub.apply _)
  
  }
  
  private case class AnnotationBodyStub(
    hasType: AnnotationBody.Type,
    lastModifiedBy: Option[String],
    lastModifiedAt: Option[DateTime],
    value: Option[String],
    uri: Option[String],
    status: Option[AnnotationStatusStub]
  ) {
    
    val toAnnotationBody = AnnotationBody(
      hasType,
      lastModifiedBy,
      lastModifiedAt.getOrElse(new DateTime()),
      value,
      uri,
      status.map(_.toAnnotationStatus)
    )
    
  }
  
  private object AnnotationBodyStub extends HasDate {
    
    implicit val annotationBodyStubReads: Reads[AnnotationBodyStub] = (
      (JsPath \ "type").read[AnnotationBody.Type] and
      (JsPath \ "last_modified_by").readNullable[String] and
      (JsPath \ "last_modified_at").readNullable[DateTime] and
      (JsPath \ "value").readNullable[String] and
      (JsPath \ "uri").readNullable[String] and
      (JsPath \ "status").readNullable[AnnotationStatusStub]
    )(AnnotationBodyStub.apply _)
  
  }
  
  private case class AnnotationStatusStub(
    value: AnnotationStatus.Value,
    setBy: Option[String],
    setAt: Option[DateTime]
  ) {
    
    val toAnnotationStatus = AnnotationStatus(value, setBy, setAt.getOrElse(new DateTime()))
    
  }
  
  private object AnnotationStatusStub extends HasDate  {
  
    implicit val annotationStatusStubReads: Reads[AnnotationStatusStub] = (
      (JsPath \ "value").read[AnnotationStatus.Value] and
      (JsPath \ "set_by").readNullable[String] and
      (JsPath \ "set_at").readNullable[DateTime]
    )(AnnotationStatusStub.apply _)
  
  }

  private case class AnnotationStub(
    annotationId: UUID,
    versionId: UUID,
    annotates: AnnotatedObjectStub,
    contributors: Seq[String],
    anchor: String,
    lastModifiedBy: Option[String],
    lastModifiedAt: Option[DateTime],
    bodies: Seq[AnnotationBodyStub]
  ) {
    
    def toAnnotation(docId: String, fileparts: Seq[DocumentFilepartRecord]) = Annotation(
      annotationId,
      versionId,
      annotates.toAnnotatedObject(docId, fileparts),
      contributors,
      anchor,
      lastModifiedBy,
      lastModifiedAt.getOrElse(new DateTime()),
      bodies.map(_.toAnnotationBody)
    )
    
  }
  
  private object AnnotationStub extends HasDate {
  
    implicit val annotationStubReads: Reads[AnnotationStub] = (
      (JsPath \ "annotation_id").read[UUID] and
      (JsPath \ "version_id").read[UUID] and
      (JsPath \ "annotates").read[AnnotatedObjectStub] and
      (JsPath \ "contributors").read[Seq[String]] and
      (JsPath \ "anchor").read[String] and
      (JsPath \ "last_modified_by").readNullable[String] and
      (JsPath \ "last_modified_at").readNullable[DateTime] and
      (JsPath \ "bodies").read[Seq[AnnotationBodyStub]]
    )(AnnotationStub.apply _)
  
  }

  private def parseDocumentMetadata(json: JsValue)(implicit documents: DocumentService) =    
    new DocumentRecord(
      // Reuse ID from backup or create a new one
      (json \ "id").asOpt[String].getOrElse(documents.generateRandomID()),
      (json \ "owner").as[String],
      // Reuse upload timestamp from backup or set to 'now'
      new Timestamp((json \ "uploaded_at").asOpt[DateTime].getOrElse(new DateTime()).getMillis),
      (json \ "title").as[String],
      (json \ "author").asOpt[String].getOrElse(null),
      null, // TODO date_numeric
      (json \ "date_freeform").asOpt[String].getOrElse(null),
      (json \ "description").asOpt[String].getOrElse(null),
      (json \ "language").asOpt[String].getOrElse(null),
      (json \ "source").asOpt[String].getOrElse(null),
      (json \ "edition").asOpt[String].getOrElse(null),
      (json \ "license").asOpt[String].getOrElse(null),
      // Default to 'false'
      (json \ "is_public").asOpt[Boolean].getOrElse(false).asInstanceOf[Boolean])
    
  private def parseFilepartMetadata(documentId: String, json: JsValue) =
    (json \ "parts").as[Seq[JsObject]].zipWithIndex.map { case (obj, idx) =>
      new DocumentFilepartRecord(
        // Reuse from backup or create a new one
        (obj \ "id").asOpt[UUID].getOrElse(UUID.randomUUID),
        documentId,
        (obj \ "title").as[String],
        (obj \ "content_type").as[String],
        (obj \ "filename").as[String],
        idx + 1)
    }
  
  // TODO instead of just logging error, forward them to the UI
  private def parseAnnotation(json: String) = {
    val result = Json.fromJson[AnnotationStub](Json.parse(json))
    if (result.isError)
      Logger.error(result.toString)
    
    result.get
  }
 
  def restoreFromZip(file: File, annotationService: AnnotationService, documentService: DocumentService)(implicit ctx: ExecutionContext) = {
    val zipFile = new ZipFile(file)
    val entries = zipFile.entries.asScala.toSeq.filter(!_.getName.startsWith("__MACOSX")) // Damn you Apple!
    
    val metadataEntry = entries.filter(_.getName == "metadata.json").head
    val metadataJson  = Json.parse(Source.fromInputStream(zipFile.getInputStream(metadataEntry), "UTF-8").getLines.mkString("\n"))
    
    val documentRecord  = parseDocumentMetadata(metadataJson)(documentService)
    val filepartRecords = parseFilepartMetadata(documentRecord.getId, metadataJson) 
    
    val fileparts = filepartRecords.map { record =>
      val entry = entries.filter(_.getName == "parts" + File.separator + record.getFilename).head
      val inputstream = zipFile.getInputStream(entry) 
      (record, inputstream)
    }
    
    val annotationEntry = entries.filter(_.getName == "annotations.jsonl").head
    val annotationLines = Source.fromInputStream(zipFile.getInputStream(annotationEntry), "UTF-8").getLines
    val annotations = annotationLines.map(parseAnnotation).toSeq.map(stub =>
      stub.toAnnotation(documentRecord.getId, filepartRecords))

    for {
     _ <- documentService.importDocument(documentRecord, fileparts)
     _ <- annotationService.insertOrUpdateAnnotations(annotations)
    } yield Unit

  }
  
  def restoreFromJSONL(file: File, annotationService: AnnotationService)(implicit ctx: ExecutionContext) = {
    val annotations = Source.fromInputStream(new FileInputStream(file)).getLines.map(line =>
      Json.fromJson[Annotation](Json.parse(line)).get)
    annotationService.insertOrUpdateAnnotations(annotations.toSeq).map(_.size == 0)
  }
  
}