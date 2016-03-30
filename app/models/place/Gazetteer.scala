package models.place

import java.io.{ File, FileInputStream, InputStream }
import java.util.zip.GZIPInputStream
import org.joda.time.DateTime
import org.pelagios.Scalagios
import org.pelagios.api.gazetteer.{ Place => PelagiosPlace }
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

  def normalizeURI(uri: String) = {
    // We remove '#this' suffixes
    val noThis = if (uri.indexOf("#this") > -1) uri.substring(0, uri.indexOf("#this")) else uri
      
    // By convention, we remove trailing slash
    if (noThis.endsWith("/"))
      noThis.substring(0, noThis.size - 1)
    else 
      noThis
  }
    
  def loadFromRDF(file: File, gazetteerName: String): Seq[GazetteerRecord] = {
    val (is, filename) = if (file.getName.endsWith(".gz"))
        (new GZIPInputStream(new FileInputStream(file)), file.getName.substring(0, file.getName.lastIndexOf('.')))
      else
        (new FileInputStream(file), file.getName)
        
    Scalagios.readPlaces(is, filename).map(p =>
      GazetteerRecord(
        normalizeURI(p.uri),
        Gazetteer(gazetteerName),
        p.label,
        p.category.map(category => Seq(category.toString)).getOrElse(Seq.empty[String]),
        p.descriptions.map(l => Description(l.chars, l.lang)),
        p.names.map(l => Name(l.chars, l.lang)),
        p.location.map(_.geometry),
        p.location.map(_.pointLocation),
        p.temporalCoverage.map(t => TemporalBounds(new DateTime(t.start), t.end.map(end => new DateTime(end)).getOrElse(new DateTime(t.start)))),
        p.closeMatches,
        p.exactMatches)).toSeq
  }
    
}
