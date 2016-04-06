package models.place

import java.io.{ File, FileInputStream, InputStream }
import java.util.zip.GZIPInputStream
import org.joda.time.{ DateTime, DateTimeZone }
import org.pelagios.Scalagios
import org.pelagios.api.gazetteer.{ Place => PelagiosPlace }
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.Logger

case class Gazetteer(name: String)

object Gazetteer {

  implicit val gazetteerFormat: Format[Gazetteer] =
    Format(
      JsPath.read[String].map(Gazetteer(_)),
      Writes[Gazetteer](t => JsString(t.name))
    )
    
  def loadFromRDF(file: File, gazetteerName: String): Seq[GazetteerRecord] = {
    val (is, filename) = if (file.getName.endsWith(".gz"))
        (new GZIPInputStream(new FileInputStream(file)), file.getName.substring(0, file.getName.lastIndexOf('.')))
      else
        (new FileInputStream(file), file.getName)
        
    loadFromRDF(is, filename, gazetteerName)
  }
    
  def loadFromRDF(is: InputStream, filename: String, gazetteerName: String): Seq[GazetteerRecord] = {

    import org.pelagios.api.PeriodOfTime
    
    // Helper to convert between Scalagios and Recogito time format
    def convertPeriodOfTime(period: PeriodOfTime): TemporalBounds = {
        val startDate = period.start
        val endDate = period.end.getOrElse(startDate)
        
        TemporalBounds(
          new DateTime(startDate).withZone(DateTimeZone.UTC), 
          new DateTime(endDate).withZone(DateTimeZone.UTC))          
    }
        
    Scalagios.readPlaces(is, filename).map(p =>
      GazetteerRecord(
        GazetteerUtils.normalizeURI(p.uri),
        Gazetteer(gazetteerName),
        DateTime.now().withZone(DateTimeZone.UTC),
        p.label,
        p.category.map(category => Seq(category.toString)).getOrElse(Seq.empty[String]),
        p.descriptions.map(l => Description(l.chars, l.lang)),
        p.names.map(l => Name(l.chars, l.lang)),
        p.location.map(_.geometry),
        p.location.map(_.pointLocation),
        p.temporalCoverage.map(convertPeriodOfTime(_)),
        p.closeMatches,
        p.exactMatches)).toSeq
  }
    
}

object GazetteerUtils {
  
  /** Normalizes a URI to a standard format
    * 
    * Removes '#this' suffixes (used by Pleiades) and, by convention, trailing slashes. 
    */
  def normalizeURI(uri: String) = {
    val noThis = if (uri.indexOf("#this") > -1) uri.substring(0, uri.indexOf("#this")) else uri
      
    if (noThis.endsWith("/"))
      noThis.substring(0, noThis.size - 1)
    else 
      noThis
  }
  
  /** Returns a clone of the gazetteer record, with all URIs normalized **/
  def normalizeRecord(r: GazetteerRecord) = 
    GazetteerRecord(
      normalizeURI(r.uri),
      r.sourceGazetteer,
      r.lastChangedAt,
      r.title,
      r.placeTypes,
      r.descriptions,
      r.names,
      r.geometry,
      r.representativePoint,
      r.temporalBounds,
      r.closeMatches.map(normalizeURI(_)),
      r.exactMatches.map(normalizeURI(_)))
      
}
