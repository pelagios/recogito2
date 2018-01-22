package services.entity.crosswalks.rdf

import java.io.{File, FileInputStream, InputStream}
import services.entity._
import org.joda.time.{DateTime, DateTimeZone}
import org.pelagios.Scalagios
import org.pelagios.api.PeriodOfTime

object PelagiosRDFCrosswalk {

  private def toLinks(uris: Seq[String], linkType: LinkType.Value) = 
    uris.map(uri => Link(EntityRecord.normalizeURI(uri), linkType))
  
  private def convertPeriodOfTime(period: PeriodOfTime): TemporalBounds = {
    val startDate = period.start
    val endDate = period.end.getOrElse(startDate)
    TemporalBounds(
      new DateTime(startDate).withZone(DateTimeZone.UTC),
      new DateTime(endDate).withZone(DateTimeZone.UTC))
  }

  def fromRDF(filename: String): InputStream => Seq[EntityRecord] = {

    val source = filename.substring(0, filename.indexOf('.'))

    def convertPlace(place: org.pelagios.api.gazetteer.Place) =
      EntityRecord(
        EntityRecord.normalizeURI(place.uri),
        source,
        DateTime.now().withZone(DateTimeZone.UTC),
        None,
        place.label,
        place.descriptions.map(l => Description(l.chars, l.lang)),
        place.names.map(l => Name(l.chars, l.lang)),
        place.location.map(_.geometry),
        place.location.map(_.pointLocation),
        None, // country code
        place.temporalCoverage.map(convertPeriodOfTime(_)),
        place.category.map(category => Seq(category.toString)).getOrElse(Seq.empty[String]),
        None, // priority
        {
          toLinks(place.closeMatches, LinkType.CLOSE_MATCH) ++
          toLinks(place.exactMatches, LinkType.EXACT_MATCH)
        })

    // Return crosswalk function
    { stream: InputStream =>
      Scalagios.readPlaces(stream, filename).map(convertPlace).toSeq }
  }

  def readFile(file: File): Seq[EntityRecord] =
    fromRDF(file.getName)(new FileInputStream(file))

}
