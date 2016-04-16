package models.place

import com.vividsolutions.jts.geom.{ Coordinate, GeometryFactory }
import java.io.File
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class GazetteerUtilSpec extends Specification {
  
  private val GAZETTEER_RDF = "test/resources/models/place/gazetteer_sample_pleiades.ttl"
  
  "The Gazetteer utility" should {
    
    val records = GazetteerUtils.loadRDF(new File(GAZETTEER_RDF), "Pleiades")
    
    "properly load all gazetteer records from RDF" in {
      records.size must equalTo(5)
      
      val expectedTitles = Seq(
        "Col. Barcino",
        "Mun. Vindobona",
        "Vindobona",
        "Thessalonica",
        "Lancaster")
      records.map(_.title) must containAllOf(expectedTitles)
    }
    
    "normalize URIs correctly" in {
      val expectedURIs = Seq(
        "http://pleiades.stoa.org/places/246343",
        "http://pleiades.stoa.org/places/128460",
        "http://pleiades.stoa.org/places/128537",
        "http://pleiades.stoa.org/places/491741",
        "http://pleiades.stoa.org/places/89222")
      records.map(_.uri) must containAllOf(expectedURIs)
    }
    
    /*
    
    "list the expected name labels, sorted by frequency" in {
      val labels = parseResult.get.labels
      
      labels.size must equalTo(4)
      labels(0) must equalTo("Ad Mauros")
      labels(1) must equalTo("Eferding")
      Seq(labels(2), labels(3)) must containAllOf(Seq("Ad Mauros/Marinianio", "Marianianio"))
    }
     */
    
    "properly import all properties of the test record" in {
      val testRecord = records.find(_.uri == "http://pleiades.stoa.org/places/128460").get
      
      testRecord.sourceGazetteer must equalTo (Gazetteer("Pleiades"))
      
      testRecord.title must equalTo("Mun. Vindobona")

      testRecord.placeTypes.size must equalTo(1)
      testRecord.placeTypes.head must equalTo("SETTLEMENT")
      
      testRecord.descriptions.size must equalTo(1)
      testRecord.descriptions.head must equalTo(Description("An ancient place, cited: BAtlas 13 B4 Mun. Vindobona"))
      
      val expectedNames = Seq(
          Name("Mun. Vindobona"),
          Name("Wien"),
          Name("Wien/Vienna AUS"))
      testRecord.names.size must equalTo(3)
      testRecord.names must containAllOf(expectedNames)
      
      val coord = new Coordinate(16.373064, 48.208982)
      val point = new GeometryFactory().createPoint(coord)
      testRecord.geometry must equalTo(Some(point))
      testRecord.representativePoint must equalTo(Some(coord))

      testRecord.temporalBounds must equalTo(Some(TemporalBounds.fromYears(-20, 630)))
      testRecord.closeMatches.size must equalTo(0)
      testRecord.exactMatches.size must equalTo(0)
      testRecord.allMatches.size must equalTo(0)
    }
    
  }
  
}