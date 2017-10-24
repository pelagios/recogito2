package models.image

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
        val angleDeg = 180 * tbox.a / Math.PI
                
        s"vips similarity $croppedFile $rotatedFile --angle $angleDeg" !

        // TODO can rotate and crop happen in the same vips command?
        val clippedTmp = new TemporaryFile(new File(TMP, annotation.annotationId + ".clip.jpg"))
        val clippedFile = clippedTmp.file.getAbsolutePath
        
        val a =  ImageAnchor.getQuadrant(tbox.a) match {
          case ImageAnchor.QUADRANT_1 | ImageAnchor.QUADRANT_3 => tbox.a
          case ImageAnchor.QUADRANT_2 => tbox.a - Math.PI / 2
          case ImageAnchor.QUADRANT_4 => Math.PI / 2 - tbox.a
        }

        val k = Math.sin(a) * Math.cos(a)
        val x = Math.abs(tbox.h * k).toInt
        val y = Math.abs(tbox.l * k).toInt
        val w = tbox.l
        val h = Math.abs(tbox.h)
        
        s"vips crop $rotatedFile $clippedFile $x $y $w $h" ! 
        
        clippedTmp.file
        
      case _ => croppedTmp.file
    }
    
  }

}
