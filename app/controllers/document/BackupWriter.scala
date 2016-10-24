package controllers.document

import controllers.HasConfig
import java.io.{ File, FileInputStream, FileOutputStream, BufferedInputStream, ByteArrayInputStream, InputStream, PrintWriter }
import java.math.BigInteger
import java.security.{ MessageDigest, DigestInputStream }
import java.util.UUID
import java.util.zip.{ ZipEntry, ZipOutputStream }
import models.HasDate
import models.annotation.{ Annotation, AnnotationService }
import models.document.DocumentInfo
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.Files.TemporaryFile
import scala.concurrent.{ ExecutionContext, Future }
import storage.Uploads
import java.security.DigestInputStream

trait BackupWriter extends HasDate with HasBackupValidation { self: HasConfig =>
  
  private val TMP = System.getProperty("java.io.tmpdir")
  
  import BackupWriter._
  
  private def writeToZip(inputStream: InputStream, filename: String, zip: ZipOutputStream) = {
    zip.putNextEntry(new ZipEntry(filename))
     
    val md = MessageDigest.getInstance(ALGORITHM)    
    val in = new DigestInputStream(new BufferedInputStream(inputStream), md)
    
    var b = in.read()
    while (b > -1) {
      zip.write(b)
      b = in.read()
    }

    in.close()
    zip.closeEntry()
    
    new BigInteger(1, md.digest()).toString(16)
  }
  
  def createBackup(doc: DocumentInfo)(implicit ctx: ExecutionContext, uploads: Uploads, annotations: AnnotationService): Future[File] = {
    
    def getFileAsStream(owner: String, documentId: String, filename: String) = {
      val dir = uploads.getDocumentDir(owner, documentId).get // Fail hard if the dir doesn't exist
      new FileInputStream(new File(dir, filename))
    }
    
    def getManifestAsStream() = {
      val manifest = "Recogito-Version: 2.0.1-alpha"
      new ByteArrayInputStream(manifest.getBytes)
    }
    
    def getMetadataAsStream(doc: DocumentInfo) = {
      val json = Json.prettyPrint(Json.toJson((doc.document, doc.fileparts)))
      new ByteArrayInputStream(json.getBytes)
    }
    
    def getAnnotationsAsStream(docId: String, annotations: Seq[Annotation], parts: Seq[DocumentFilepartRecord]): InputStream = {
      val tmp = new TemporaryFile(new File(TMP, docId + "_annotations.jsonl"))
      val writer = new PrintWriter(tmp.file)
      annotations.foreach(a => writer.println(Json.stringify(Json.toJson(a))))
      writer.close()
      new FileInputStream(tmp.file)
    }
    
    Future {
      new TemporaryFile(new File(TMP, doc.id + ".zip"))
    } flatMap { zipFile =>
      val zipStream = new ZipOutputStream(new FileOutputStream(zipFile.file))

      writeToZip(getManifestAsStream(), "manifest", zipStream)
      val metadataHash = writeToZip(getMetadataAsStream(doc), "metadata.json", zipStream)

      val fileHashes = doc.fileparts.map { part =>
        writeToZip(getFileAsStream(doc.ownerName, doc.id, part.getFile), "parts" + File.separator + part.getFile, zipStream)
      }

      annotations.findByDocId(doc.id).map { annotations =>
        val annotationsHash = writeToZip(getAnnotationsAsStream(doc.id, annotations.map(_._1), doc.fileparts), "annotations.jsonl", zipStream)
        
        val signature = computeSignature(metadataHash, fileHashes, annotationsHash)
        writeToZip(new ByteArrayInputStream(signature.getBytes), "signature", zipStream)
        
        zipStream.close()
        zipFile.file
      }
    }
  }
  
}

object BackupWriter {
  
  implicit val documentRecordWrites: Writes[DocumentRecord] = (
    (JsPath \ "id").write[String] and
    (JsPath \ "owner").write[String] and
    (JsPath \ "uploaded_at").write[DateTime] and
    (JsPath \ "title").write[String] and
    (JsPath \ "author").writeNullable[String] and
    // TODO date_numeric
    (JsPath \ "date_freeform").writeNullable[String] and
    (JsPath \ "description").writeNullable[String] and
    (JsPath \ "language").writeNullable[String] and
    (JsPath \ "source").writeNullable[String] and
    (JsPath \ "edition").writeNullable[String] and
    (JsPath \ "is_public").write[Boolean]
  )(d => (
    d.getId,
    d.getOwner,
    new DateTime(d.getUploadedAt.getTime),
    d.getTitle,
    Option(d.getAuthor),
    // TODO date_numeric
    Option(d.getDateFreeform),
    Option(d.getDescription),
    Option(d.getLanguage),
    Option(d.getSource),
    Option(d.getEdition),
    d.getIsPublic
  ))

  implicit val documentFilepartRecordWrites: Writes[DocumentFilepartRecord] = (
    (JsPath \ "id").write[UUID] and
    (JsPath \ "title").write[String] and
    (JsPath \ "content_type").write[String] and
    (JsPath \ "filename").write[String]
  )(p => (
    p.getId,
    p.getTitle,
    p.getContentType,
    p.getFile
  ))
  
  implicit val metadataWrites: Writes[(DocumentRecord, Seq[DocumentFilepartRecord])] = (
    (JsPath).write[DocumentRecord] and
    (JsPath \ "parts").write[Seq[DocumentFilepartRecord]]
  )(tuple => (tuple._1, tuple._2))
  
}