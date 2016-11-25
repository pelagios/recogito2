package controllers.document

import collection.JavaConverters._
import controllers.HasConfig
import java.io.{ File, InputStream }
import java.sql.Timestamp
import java.util.UUID
import java.util.zip.ZipFile
import models.{ ContentType, HasDate, HasContentTypeList }
import models.annotation._
import models.document.DocumentService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.{ ExecutionContext, Future }
import scala.io.Source

trait BackupReader extends HasDate with HasBackupValidation { self: HasConfig =>
  
  import BackupReader._
  
  private def openZipFile(file: File) = {
    val zipFile = new ZipFile(file)
    (zipFile, zipFile.entries.asScala.toSeq.filter(!_.getName.startsWith("__MACOSX")))
  }
  
  def readMetadata(file: File, runAsAdmin: Boolean, forcedOwner: Option[String])(implicit ctx: ExecutionContext, documentService: DocumentService) = Future {
    
    def parseDocumentMetadata(json: JsValue) = {
      // User ID from backup, or create new if allowed (for interop with legacy backups)
      val id = (json \ "id").asOpt[String]
        .getOrElse { 
          if (!runAsAdmin) // Only admins may import legacy backups
            throw new HasBackupValidation.InvalidBackupException
            
          documentService.generateRandomID() 
        }
      
      if (documentService.existsId(id))
        throw new HasBackupValidation.DocumentExistsException
        
      val owner = forcedOwner match {
          // Personal restore always forces the document owner to the logged-in user
          case Some(username) => username

          // Only admins can retain the user from the backup metadata - but 
          // forceOwner == None && runAsAdmin == false should never happen
          case _ =>
            if (!runAsAdmin)
              throw new RuntimeException

            (json \ "owner").as[String]
        }

      new DocumentRecord(
        // Reuse ID from backup or create a new one
        id,
        owner,
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
    }
    
    def parseFilepartMetadata(documentId: String, json: JsValue) =
      (json \ "parts").as[Seq[JsObject]].zipWithIndex.map { case (obj, idx) =>
        new DocumentFilepartRecord(
          // Reuse from backup or create a new one
          (obj \ "id").asOpt[UUID].getOrElse(UUID.randomUUID),
          documentId,
          (obj \ "title").as[String],
          (obj \ "content_type").as[String],
          (obj \ "file").as[String],
          idx + 1,
          (obj \ "source").asOpt[String].getOrElse(null))
      }
          
    scala.concurrent.blocking {      
      val (zipFile, entries) = openZipFile(file)
      val metadataEntry = entries.filter(_.getName == "metadata.json").head
      val metadataJson  = Json.parse(Source.fromInputStream(zipFile.getInputStream(metadataEntry), "UTF-8").getLines.mkString("\n"))
        
      val documentRecord  = parseDocumentMetadata(metadataJson)
      val filepartRecords = parseFilepartMetadata(documentRecord.getId, metadataJson) 
      
      (documentRecord, filepartRecords)
    }
  }
  
  def readAnnotations(file: File)(implicit ctx: ExecutionContext) = Future {
      
    def parseAnnotation(json: String) =
      Json.fromJson[AnnotationStub](Json.parse(json)).get
    
    scala.concurrent.blocking {
      val (zipFile, entries) = openZipFile(file)
      val annotationEntry = entries.filter(_.getName == "annotations.jsonl").head
      Source.fromInputStream(zipFile.getInputStream(annotationEntry), "UTF-8").getLines.map(parseAnnotation)
    }
  }
  
  def restoreBackup(
      file: File,
      runAsAdmin: Boolean,
      forcedOwner: Option[String]
    )(implicit ctx: ExecutionContext, annotationService: AnnotationService, documentService: DocumentService) = {
    
    def restoreDocument(document: DocumentRecord, parts: Seq[DocumentFilepartRecord]) = {
      val (zipFile, entries) = openZipFile(file)
      
      val fileparts = parts.map { part => 
        val entry = entries.filter(_.getName == "parts" + File.separator + part.getFile).head
        val stream = zipFile.getInputStream(entry)
        (part, stream)
      }
      
      documentService.importDocument(document, fileparts) 
    }
    
    def restoreAnnotations(annotationStubs: Iterator[AnnotationStub], docId: String, fileparts: Seq[DocumentFilepartRecord]) = {
      val filepartIds = fileparts.map(_.getId) 
        
      val annotations = annotationStubs
        .map(stub => stub.toAnnotation(docId, fileparts))
        .filter(annotation => filepartIds.contains(annotation.annotates.filepartId))
 
      annotationService.insertOrUpdateAnnotations(annotations.toSeq)
    }
    
    def restore() = {
      val fReadMetadata = readMetadata(file, runAsAdmin, forcedOwner)
      val fReadAnnotations = readAnnotations(file)
      
      for {
        (document, parts) <- fReadMetadata
        annotationStubs <- fReadAnnotations
        _ <- restoreDocument(document, parts)
        _ <- restoreAnnotations(annotationStubs, document.getId, parts)
      } yield Unit
    }
    
    if (runAsAdmin) // Admins can just restore...
      restore()
    else // ...anyone else needs to go through validation first
      validateBackup(file).flatMap { valid =>
        if (!valid)
          throw new HasBackupValidation.InvalidSignatureException
          
        restore()
      }
  }
  
}

object BackupReader extends HasDate with HasContentTypeList {
  
  case class AnnotatedObjectStub(
    documentId: Option[String],
    filepartId: Option[UUID],
    filepartTitle: Option[String],
    contentType: ContentType
  ) {
    
    def toAnnotatedObject(docId: String, fileparts: Seq[DocumentFilepartRecord]) = AnnotatedObject(
      docId,
      filepartId.getOrElse(fileparts.find(_.getTitle.equals(filepartTitle.get)).get.getId),
      contentType: ContentType
    )
    
  }
  
  private implicit val annotatedObjectStubReads: Reads[AnnotatedObjectStub] = (
    (JsPath \ "document_id").readNullable[String] and
    (JsPath \ "filepart_id").readNullable[UUID] and
    (JsPath \ "filepart_title").readNullable[String] and
    (JsPath \ "content_type").read[JsValue].map(fromCTypeList)
  )(AnnotatedObjectStub.apply _)
  
  
  private implicit val annotationStatusStubReads: Reads[AnnotationStatusStub] = (
    (JsPath \ "value").read[AnnotationStatus.Value] and
    (JsPath \ "set_by").readNullable[String] and
    (JsPath \ "set_at").readNullable[DateTime]
  )(AnnotationStatusStub.apply _)
  
  
  case class AnnotationBodyStub(
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
    
  private implicit val annotationBodyStubReads: Reads[AnnotationBodyStub] = (
    (JsPath \ "type").read[AnnotationBody.Type] and
    (JsPath \ "last_modified_by").readNullable[String] and
    (JsPath \ "last_modified_at").readNullable[DateTime] and
    (JsPath \ "value").readNullable[String] and
    (JsPath \ "uri").readNullable[String] and
    (JsPath \ "status").readNullable[AnnotationStatusStub]
  )(AnnotationBodyStub.apply _)
  
  case class AnnotationStatusStub(
    value: AnnotationStatus.Value,
    setBy: Option[String],
    setAt: Option[DateTime]
  ) {
    
    val toAnnotationStatus = AnnotationStatus(value, setBy, setAt.getOrElse(new DateTime()))
    
  }
  
  case class AnnotationStub(
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
  
  private implicit val annotationStubReads: Reads[AnnotationStub] = (
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
