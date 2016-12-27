package controllers.api.image

import scala.language.implicitConversions

sealed trait ImageAnchor {
  
  case class Bounds(x: Int, y: Int, width: Int, height: Int)
  
  def bounds: Bounds
  
}

case class PointAnchor(x: Int, y: Int) extends ImageAnchor {
  
  private val WIDTH = 120
  
  private val HEIGHT = 120
  
  val bounds = Bounds(x - WIDTH / 2, y - HEIGHT / 2, WIDTH, HEIGHT)
  
}

case class RectAnchor(x: Int, y: Int, w: Int, h: Int) extends ImageAnchor {
  
  val bounds = Bounds(x, y, w, h)
  
}

case class TiltedBoxAnchor(x: Int, y: Int, a: Double, l: Int, h: Int) extends ImageAnchor {
  
  implicit def doubleToInt(d: Double) = d.toInt
  
  val bounds = {
    
    def boundsQ1() = Bounds(
      x - h * Math.sin(a),
      y - l * Math.sin(a) - h * Math.cos(a),
      l * Math.cos(a) + h * Math.sin(a),
      l * Math.sin(a) + h * Math.cos(a))
    
    def boundsQ2() = ???
    
    def boundsQ3() = ???
    
    def boundsQ4() = ??? 
    
    a match {
      case a if a >= 0 && a < Math.PI / 2 => boundsQ1
      case a if a < Math.PI => boundsQ2
      case a if a < 3 * Math.PI / 2 => boundsQ3
      case _ => boundsQ4
    }
  }

}

object ImageAnchor {

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
      args.get("x").get.toInt,
      args.get("y").get.toInt,
      args.get("a").get.toDouble,
      args.get("l").get.toInt,
      args.get("h").get.toInt)
  }

}
