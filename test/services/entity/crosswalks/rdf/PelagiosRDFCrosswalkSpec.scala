package services.entity.crosswalks.rdf

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory}
import java.io.{File, FileInputStream}
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import services.entity.{Description, Name, TemporalBounds}

@RunWith(classOf[JUnitRunner])
class PelagiosRDFCrosswalkSpec extends Specification {

  private val GAZETTEER_RDF = new File("test/resources/services/place/gazetteer_sample_pleiades.ttl")
  
  "The Pelagios Gazetter RDF crosswalk" should {
    
    val records = PelagiosRDFCrosswalk.readFile(GAZETTEER_RDF)
   
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
    
    "properly import all properties of the test record" in {
      val testRecord = records.find(_.uri == "http://pleiades.stoa.org/places/128460").get
      
      testRecord.sourceAuthority must equalTo ("gazetteer_sample_pleiades")
      
      testRecord.title must equalTo("Mun. Vindobona")

      testRecord.subjects.size must equalTo(1)
      testRecord.subjects.head must equalTo("SETTLEMENT")
      
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
      testRecord.links.size must equalTo(0)
    }
    
  }
  
}