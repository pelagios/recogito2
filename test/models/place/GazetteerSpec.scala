package models.place

import java.io.File
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class GazetteerSpec extends Specification {
  
  private val GAZETTEER_RDF = "test/resources/gazetteer_sample_pleiades.ttl"
  
  "The Gazetteer utility" should {
    
    "properly load gazetteer records from RDF" in {
      val records = Gazetteer.loadFromRDF(new File(GAZETTEER_RDF))
      
      // TODO implement: 
      // correct number of places? 
      // Query a few properties on each record
      // Match one sample against a full programmatic representation
      
      failure
    }
    
  }
  
}