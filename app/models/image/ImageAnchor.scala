package models.image

import scala.language.implicitConversions

sealed trait ImageAnchor {
  
  case class Bounds(left: Int, top: Int, right: Int, bottom: Int) {
    
    val width = right - left
    
    val height = bottom - top
    
  }
    
  def bounds: Bounds
  
}

case class PointAnchor(x: Int, y: Int) extends ImageAnchor {
  
  private val WIDTH = 120
  
  private val HEIGHT = 120
  
  val bounds = Bounds(x - WIDTH / 2, y - HEIGHT / 2, x + WIDTH / 2, y + HEIGHT / 2)
  
}

case class RectAnchor(x: Int, y: Int, w: Int, h: Int) extends ImageAnchor {
  
  val bounds = (w, h) match {
    case (w, h) if w > 0 && h > 0 => Bounds(x, y, x + w, y + h)
    case (w, h) if w < 0 && h > 0 => Bounds(x + w, y, x, y + h)
    case (w, h) if w > 0 && h < 0 => Bounds(x, y + h, x + w, y)
    case (w, h) if w < 0 && h < 0 => Bounds(x + w, y + h, x, y)
  }

  
}

case class TiltedBoxAnchor(x: Int, y: Int, a: Double, l: Int, h: Int) extends ImageAnchor {
  
  implicit def doubleToInt(d: Double) = d.toInt
    
  val bounds = {
    
    def boundsQ1() = { 
      val sinA = Math.sin(a)
      val cosA = Math.cos(a)
      
      if (h > 0)
        Bounds(
          x - h * sinA,
          y - l * sinA - h * cosA,
          x + l * cosA,
          y)
      else
        Bounds(
          x,
          y - l * sinA,
          x + l * cosA - h * sinA,
          y - h * cosA)
    }
    
    def boundsQ2() = {
      val sinB = Math.sin(Math.PI - a)
      val cosB = Math.cos(Math.PI - a)
      
      if (h > 0)
        Bounds(
          x - l * cosB - h * sinB,
          y - l * sinB,
          x,
          y + h * cosB)
      else
        Bounds(
          x - l * cosB,
          y - l * sinB + h * cosB,
          x - h * sinB,
          y)
    }
    
    def boundsQ3() = {      
      val sinG = Math.sin(Math.PI + a)
      val cosG = Math.cos(Math.PI + a)
      
      if (h > 0)
        Bounds(
          x - l * cosG,
          y,
          x + h * sinG,
          y + l * sinG + h * cosG)
      else
        Bounds(
          x - l * cosG + h * sinG,
          y + h * cosG,
          x,
          y + l * sinG)

    }
    
    def boundsQ4() = {
      val sinD = Math.sin(-a)
      val cosD = Math.cos(-a)
      
      if (h > 0)
        Bounds(
          x,
          y - h * cosD,
          x + l * cosD + h * sinD,
          y + l * sinD)
      else
        Bounds(
          x + h * sinD,
          y,
          x + l * cosD,
          y + l * sinD - h * cosD)

    }
    
    // play.api.Logger.info("a=" + 180 * a / Math.PI)
    
    ImageAnchor.getQuadrant(a) match {
      case ImageAnchor.QUADRANT_1 => boundsQ1
      case ImageAnchor.QUADRANT_2 => boundsQ2
      case ImageAnchor.QUADRANT_3 => boundsQ3
      case ImageAnchor.QUADRANT_4 => boundsQ4
    }
  }

}

object ImageAnchor {

  sealed trait QUADRANT
  case object QUADRANT_1 extends QUADRANT
  case object QUADRANT_2 extends QUADRANT
  case object QUADRANT_3 extends QUADRANT
  case object QUADRANT_4 extends QUADRANT

  def getQuadrant(rad: Double) = rad match {
    case a if a >= 0 && a < Math.PI / 2 => QUADRANT_1
    case a if a >= 0 && a < Math.PI => QUADRANT_2
    case a if a < 0 && a < - Math.PI / 2 => QUADRANT_3
    case _ => QUADRANT_4
  }  
  
  def parse(anchor: String): ImageAnchor =
    anchor.substring(0, anchor.indexOf(':')) match {
      case "point" => parsePointAnchor(anchor)
      case "rect"  => parseRectAnchor(anchor)
      case "tbox"  => parseTiltedBoxAnchor(anchor)
    }
  
  private def parseArgs(anchor: String) = 
    anchor.substring(anchor.indexOf(':') + 1).split(',').map(arg =>
      (arg.substring(0, arg.indexOf('=')) -> arg.substring(arg.indexOf('=') + 1))).toMap

  // Eg. point:1099,1018
  def parsePointAnchor(anchor: String) = {
    val args = anchor.substring(6).split(',').map(_.toInt)
    PointAnchor(args(0), args(1))
  }

  // Eg. rect:x=3184,y=1131,w=905,h=938
  def parseRectAnchor(anchor: String) = {
    val args = parseArgs(anchor)
    RectAnchor(
      args.get("x").get.toInt,
      args.get("y").get.toInt,
      args.get("w").get.toInt,
      args.get("h").get.toInt)
  }

  // Eg. tbox:x=3713,y=4544,a=0.39618258447890137,l=670,h=187
  def parseTiltedBoxAnchor(anchor: String) = {
    val args = parseArgs(anchor)
    TiltedBoxAnchor(
      Math.round(args.get("x").get.toFloat),
      Math.round(args.get("y").get.toFloat),
      args.get("a").get.toDouble,
      Math.round(args.get("l").get.toFloat),
      Math.round(args.get("h").get.toFloat))
  }

}
