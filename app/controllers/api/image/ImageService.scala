package controllers.api.image

import models.annotation.Annotation
import models.document.DocumentInfo
import scala.util.Try

sealed trait ImageAnchor
case class PointAnchor(x: Int, y: Int) extends ImageAnchor
case class RectAnchor(x: Int, y: Int, w: Int, h: Int) extends ImageAnchor
case class TiltedBoxAnchor(x: Int, y: Int, a: Double, l: Int, h: Int) extends ImageAnchor

object ImageAnchor {

  def parse(anchor: String): Option[ImageAnchor] =
    anchor.substring(0, anchor.indexOf(':')) match {
      case "point" => parsePointAnchor(anchor)
      case "rect"  => parseRectAnchor(anchor)
      case "tbox"  => parseTiltedBoxAnchor(anchor)
    }

  // point:1099,1018
  def parsePointAnchor(anchor: String): Option[PointAnchor] = {
    val maybePoint = Try(anchor.substring(6).split(',').map(_.toInt))
    if (maybePoint.isSuccess)
      Some(PointAnchor(maybePoint.get(0), maybePoint.get(1)))
    else
      None
  }

  // rect:x=3184,y=1131,w=905,h=938
  def parseRectAnchor(anchor: String): Option[RectAnchor] = ???

  // tbox:x=3713,y=4544,a=0.39618258447890137,l=670,h=187
  def parseTiltedBoxAnchor(anchor: String): Option[TiltedBoxAnchor] = ???

}

object ImageService {

  // left = x - h * sin a
  // top = y - l * sin a - h * cos a
  // right = x + l * cos a
  // bottom = y

  // vips crop egerton_ms_2855_f004r.tif cropped.tif 1000 500 300 150
  // vips similarity cropped.tif rot.tif --angle 30

  def getContext(doc: DocumentInfo, annotation: Annotation) {
    play.api.Logger.info(ImageAnchor.parse(annotation.anchor).toString)
    /*
    Future {
      s"vips dzsave $file $destFolder --layout zoomify" !
    } map { result =>
      if (result == 0)
        Unit
      else
        throw new Exception("Image tiling failed for " + file.getAbsolutePath + " to " + destFolder.getAbsolutePath)
    }*/
  }

}
