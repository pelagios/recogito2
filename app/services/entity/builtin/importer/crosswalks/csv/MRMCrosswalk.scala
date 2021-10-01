package services.entity.builtin.importer.crosswalks.csv

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory}
import java.io.InputStream
import scala.io.Source
import services.entity.{Description, EntityRecord, Link, Name}
import kantan.csv.CsvConfiguration
import kantan.csv.CsvConfiguration.{Header, QuotePolicy}
import kantan.csv.ops._
import kantan.csv.engine.commons._
import kantan.codecs.Result.Success
import org.joda.time.DateTime

object MRMCrosswalk {
  
  val config = CsvConfiguration(',', '"', QuotePolicy.WhenNeeded, Header.None)

  def fromCSV(record: String): Option[EntityRecord] = {

    val fields = record.asCsvReader[List[String]](config).toIterator.next.get
    val uri = fields(1)

    if (uri.startsWith("http")) {
      val coord = new Coordinate(
        fields(7).toDouble,
        fields(6).toDouble
      )

      Some(EntityRecord(
        uri,
        "https://www.wikidata.org",
        DateTime.now(),
        None, // lastChangedAt
        fields(3), // title
        Seq(new Description(fields(4))),
        Seq.empty[Name],
        Some(new GeometryFactory().createPoint(coord)),
        Some(coord),
        None, // country ocde
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