package controllers.api.image

import java.io.File
import models.annotation.Annotation
import models.document.DocumentInfo
import models.generated.tables.records.DocumentFilepartRecord
import play.api.libs.Files.TemporaryFile
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import scala.util.Try
import storage.Uploads
import sys.process._
import scala.util.Success

case class Bounds(x: Int, y: Int, width: Int, height: Int)

sealed trait ImageAnchor {
  
  def bounds: Bounds
  
}

case class PointAnchor(x: Int, y: Int) extends ImageAnchor {
  
  val bounds = Bounds(x, y, 0, 0)
  
}

case class RectAnchor(x: Int, y: Int, w: Int, h: Int) extends ImageAnchor {
  
  val bounds = Bounds(x, y, w, h)
  
}

case class TiltedBoxAnchor(x: Int, y: Int, a: Double, l: Int, h: Int) extends ImageAnchor {
  
  def bounds = ???
  
}

object ImageAnchor {

  def parse(anchor: String): Option[ImageAnchor] =
    anchor.substring(0, anchor.indexOf(':')) match {
      case "point" => parsePointAnchor(anchor)
      case "rect"  => parseRectAnchor(anchor)
      case "tbox"  => parseTiltedBoxAnchor(anchor)
    }

  // point:1099,1018
  def parsePointAnchor(anchor: String): Option[PointAnchor] =
    Try(anchor.substring(6).split(',').map(_.toInt)) match {
      case Success(point) => Some(PointAnchor(point(0), point(1)))
      case _ => None
    }

  // rect:x=3184,y=1131,w=905,h=938
  def parseRectAnchor(anchor: String): Option[RectAnchor] =
    Try(anchor.substring(5).split(',').map(_.substring(2).toInt)) match {
      case Success(values) =>
        Some(RectAnchor(values(0), values(1), values(2), values(3)))
        
      case _ => None
    }

  // tbox:x=3713,y=4544,a=0.39618258447890137,l=670,h=187
  def parseTiltedBoxAnchor(anchor: String): Option[TiltedBoxAnchor] = ???

}

object ImageService {

  private val TMP = System.getProperty("java.io.tmpdir")
  
  // left = x - h * sin a
  // top = y - l * sin a - h * cos a
  // right = x + l * cos a
  // bottom = y

  // vips crop egerton_ms_2855_f004r.tif cropped.tif 1000 500 300 150
  // vips similarity cropped.tif rot.tif --angle 30

  def cutout(doc: DocumentInfo, part: DocumentFilepartRecord, annotation: Annotation)(implicit uploads: Uploads, ctx: ExecutionContext) = Future {
    val dir = uploads.getDocumentDir(doc.ownerName, doc.id).get
    
    val sourceFile = new File(dir, part.getFile)
    val tmp = new TemporaryFile(new File(TMP, annotation.annotationId + ".jpg"))
    val destFile = tmp.file.getAbsolutePath
    
    val anchor = ImageAnchor.parse(annotation.anchor).get
    val x = anchor.bounds.x
    val y = anchor.bounds.y
    val w = anchor.bounds.width
    val h = anchor.bounds.height

    s"vips crop $sourceFile $destFile $x $y $w $h" ! 
    
    tmp.file
  }

}
