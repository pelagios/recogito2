package controllers.api.image

import java.io.File
import models.annotation.Annotation
import models.document.DocumentInfo
import models.generated.tables.records.DocumentFilepartRecord
import play.api.libs.Files.TemporaryFile
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import storage.Uploads
import sys.process._

object ImageService {

  private val TMP = System.getProperty("java.io.tmpdir")

  // vips similarity cropped.tif rot.tif --angle 30

  def cutout(doc: DocumentInfo, part: DocumentFilepartRecord, annotation: Annotation)(implicit uploads: Uploads, ctx: ExecutionContext) = Future {
    val dir = uploads.getDocumentDir(doc.ownerName, doc.id).get
    
    val sourceFile = new File(dir, part.getFile)
    val croppedTmp = new TemporaryFile(new File(TMP, annotation.annotationId + ".jpg"))
    val croppedFile = croppedTmp.file.getAbsolutePath
    
    val anchor = ImageAnchor.parse(annotation.anchor)
    
    val x = anchor.bounds.left
    val y = anchor.bounds.top
    val w = anchor.bounds.width
    val h = anchor.bounds.height

    s"vips crop $sourceFile $croppedFile $x $y $w $h" ! 
    
    anchor match {
      case tbox: TiltedBoxAnchor =>    
        val rotatedTmp = new TemporaryFile(new File(TMP, annotation.annotationId + ".rot.jpg"))
        val rotatedFile = rotatedTmp.file.getAbsolutePath      
        
        val angle = 180 * tbox.a / Math.PI

        // TODO clip

        s"vips similarity $croppedFile $rotatedFile --angle $angle" !
        
        rotatedTmp.file
        
      case _ => croppedTmp.file
    }
    
  }

}
