package services.entity.builtin.importer.crosswalks.csv

import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory}
import java.io.InputStream
import scala.io.Source
import services.entity.{CountryCode, Description, EntityRecord, Link, Name}
import kantan.csv.CsvConfiguration
import kantan.csv.CsvConfiguration.{Header, QuotePolicy}
import kantan.csv.ops._
import kantan.csv.engine.commons._
import kantan.codecs.Result.Success
import org.joda.time.DateTime

object MRMCrosswalk {
  
  val config = CsvConfiguration(',', '"', QuotePolicy.WhenNeeded, Header.None)

  val reader = new WKTReader(new GeometryFactory())

  def fromCSV(identifier: String)(record: String): Option[EntityRecord] = {

    val fields = record.asCsvReader[List[String]](config).toIterator.next.get
    val uri = fields(5)

    if (uri.startsWith("http")) {
      val geom = reader.read(fields(0))

      Some(EntityRecord(
        uri,
        identifier,
        DateTime.now(),
        None, // lastChangedAt
        fields(2), // title
        Seq.empty[Description], // (new Description(fields(4))),
        Seq.empty[Name],
        Some(geom),
        Some(geom.getCentroid().getCoordinate()),
        Some(CountryCode(fields(6))), // country ocde
        None, // temporalBounds
        Seq.empty[String], // subjects
        None, // priority
        Seq.empty[Link]
      ))
    } else {
      None
    }
  }

}