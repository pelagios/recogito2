package models.place

import java.io.File
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Gazetteer(name: String)

object Gazetteer {
  
  implicit val gazetteerFormat: Format[Gazetteer] =
    Format(
      JsPath.read[String].map(Gazetteer(_)),
      Writes[Gazetteer](t => JsString(t.name))
    )
    
  def loadFromRDF(file: File): Seq[GazetteerRecord] = {
    // TODO implement
    null
  }
    
}
