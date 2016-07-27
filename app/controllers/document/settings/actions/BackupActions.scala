package controllers.document.settings.actions

import controllers.BaseAuthController
import controllers.document.settings.HasAdminAction
import java.io.{ BufferedInputStream, ByteArrayInputStream, File, FileInputStream, FileOutputStream, InputStream, PrintWriter }
import java.util.UUID
import java.util.zip.{ ZipEntry, ZipOutputStream }
import models.HasDate
import models.annotation.{ Annotation, AnnotationService }
import models.user.Roles._
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.joda.time.DateTime
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.Future
import storage.FileAccess
import java.sql.Timestamp

trait BackupActions extends HasAdminAction with FileAccess with HasDate { self: BaseAuthController =>

  private val TMP_DIR = System.getProperty("java.io.tmpdir")
  
  private implicit val executionContext = self.ctx.ec
  private implicit val elasticSearch = self.ctx.es
  
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
    p.getFilename
  ))
  
  implicit val metadataWrites: Writes[(DocumentRecord, Seq[DocumentFilepartRecord])] = (
    (JsPath).write[DocumentRecord] and
    (JsPath \ "parts").write[Seq[DocumentFilepartRecord]]
  )(tuple => (tuple._1, tuple._2)) 
  
  private def addToZip(inputStream: InputStream, filename: String, zip: ZipOutputStream) = {
    zip.putNextEntry(new ZipEntry(filename))
    
    val in = new BufferedInputStream(inputStream)
    var b = in.read()
    while (b > -1) {
      zip.write(b)
      b = in.read()
    }
    
    in.close()
    zip.closeEntry()    
  }
  
  private def exportManifest(): InputStream = {
    val manifest = "Recogito-Version: 2.0.1-alpha"
    new ByteArrayInputStream(manifest.getBytes)
  }
  
  private def exportMetadata(document: DocumentRecord, parts: Seq[DocumentFilepartRecord]): InputStream = {
    val json = Json.prettyPrint(Json.toJson((document, parts)))
    new ByteArrayInputStream(json.getBytes)
  }
  
  private def exportFile(owner: String, documentId: String, filename: String): InputStream = {
    val dir = getDocumentDir(owner, documentId).get // Fail hard if the dir doesn't exist
    new FileInputStream(new File(dir, filename))
  }
  
  private def exportAnnotations(documentId: String, annotations: Seq[Annotation]/** TODO hack **/, parts: Seq[DocumentFilepartRecord]): InputStream = {
    val tmp = new TemporaryFile(new File(TMP_DIR, documentId + "annotations.jsonl"))
    val writer = new PrintWriter(tmp.file)
    
    annotations.foreach(annotation =>
      writer.println(Json.stringify(Json.toJson(annotation))))
      
    writer.flush()
    writer.close()
    
    new FileInputStream(tmp.file)
  }
  
  def exportAsZip(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    /*
    documentAdminAction(documentId, loggedIn.user.getUsername, { case (document, parts) =>
      Future {
        new TemporaryFile(new File(TMP_DIR, documentId + ".zip"))
      } flatMap { zipFile =>
        val zipStream = new ZipOutputStream(new FileOutputStream(zipFile.file))
        
        addToZip(exportManifest(), "manifest", zipStream)
        addToZip(exportMetadata(document, parts), "metadata.json", zipStream)
        
        parts.foreach(part =>
          addToZip(exportFile(document.getOwner, documentId, part.getFilename), "parts" + File.separator + part.getFilename, zipStream))
          
        annotationService.findByDocId(documentId).map { annotations =>
          addToZip(exportAnnotations(documentId, annotations.map(_._1), parts), "annotations.jsonl", zipStream)
          zipStream.close()
          Ok.sendFile(zipFile.file) 
        }
      }
    })
    */
    null
  }
  
}