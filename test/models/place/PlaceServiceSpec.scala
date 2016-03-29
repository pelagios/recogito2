package models.place

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class PlaceServiceSpec extends Specification {
  
  // Force Specs2 to execute tests in sequential order
  sequential 
  
  "After indexing records from one gazetteer, the index" should {
    
    "contain a corresponding number of places" in {
      // TODO implement
      failure
    }
    
    "return places based on their URI" in {
      // TODO implement
      failure
    }
    
    "return places based on a search by name" in {
      // TODO implement
      failure
    }
    
    "return places based on a search by closeMatch URI" in {
      // TODO implement
      failure
    }
    
  }
  
  "After indexing records from a second gazetteers, the index" should {
    
    "properly conflate the new records with those of the first gazetteer" in {
      // TODO implement
      failure
    }

  }
  
  "After adding a 'bridiging' record that connects two places, the index" should {
    
    "contain one place less" in {
      // TODO implement
      failure
    }
    
    "should contain a 'successor place' consisting of the properly conflated records" in {
      // TODO implement
      failure
    }
    
  }
  
  "After removing the bridging record, the index" should {
    
    "contain one place more" in {
      // TODO implement
      failure
    }
    
    "should contain two 'successor places' consisting of properly conflated records" in {
      // TODO implement
      failure
    }
    
  }
  
}