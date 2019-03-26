package services.image

import java.io.File
import java.nio.file.Paths
import play.api.Configuration
import play.api.libs.Files.TemporaryFileCreator
import services.annotation.Annotation
import services.document.ExtendedDocumentMetadata
import services.generated.tables.records.DocumentFilepartRecord
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import storage.uploads.Uploads
import sys.process._
import storage.TempDir

object ImageService {

  def cutout(
    doc: ExtendedDocumentMetadata,
    part: DocumentFilepartRecord,
    annotation: Annotation
  )(implicit uploads: Uploads, ctx: ExecutionContext, tmpCreator: TemporaryFileCreator, config: Configuration) = Future {
    val tmpDir = TempDir.get()
    val dir = uploads.getDocumentDir(doc.ownerName, doc.id).get
    
    val sourceFile = new File(dir, part.getFile)
    val croppedTmp = tmpCreator.create(Paths.get(tmpDir, s"${annotation.annotationId}.jpg"))
    val croppedFile = croppedTmp.path.toAbsolutePath.toString
    
    val anchor = ImageAnchor.parse(annotation.anchor)
    
    val x = anchor.bounds.left
    val y = anchor.bounds.top
    val w = anchor.bounds.width
    val h = anchor.bounds.height
    
    s"vips crop $sourceFile $croppedFile $x $y $w $h" ! 
    
    anchor match {
      case tbox: TiltedBoxAnchor =>    
        val rotatedTmp = tmpCreator.create(Paths.get(tmpDir, s"${annotation.annotationId}.rot.jpg"))
        val rotatedFile = rotatedTmp.path.toAbsolutePath.toString
        val angleDeg = 180 * tbox.a / Math.PI
                
        s"vips similarity $croppedFile $rotatedFile --angle $angleDeg" !

        // TODO can rotate and crop happen in the same vips command?
        val clippedTmp = tmpCreator.create(Paths.get(tmpDir, s"${annotation.annotationId}.clip.jpg"))
        val clippedFile = clippedTmp.path.toAbsolutePath.toString
        
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
        
        clippedTmp.path.toFile
        
      case _ => croppedTmp.path.toFile
    }
    
  }
  
  def iiifSnippet(doc: ExtendedDocumentMetadata, part: DocumentFilepartRecord, annotation: Annotation): String = {
    val b = ImageAnchor.parse(annotation.anchor).bounds
    
    val baseUrl = 
      if (part.getFile.endsWith("/info.json"))
        part.getFile.substring(0, part.getFile.size - 10)
      else
        part.getFile
    
    // TODO rotation - don't modify the image in Recogito, rely on the external IIIF server (which may or may not support rotation)
    
    s"${baseUrl}/${b.left},${b.top},${b.width},${b.height}/full/0/default.jpg"
  }

}
