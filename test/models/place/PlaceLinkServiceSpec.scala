package models.place

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class PlaceLinkServiceSpec extends Specification {
  
  "After creating 2 annotations with 1 place link each, the PlaceLinkService" should {
    
    "contain 2 correct place links" in {
      
      // TODO are there two?
      
      // TODO are they correct?
      
      failure
    }
    
  }
  
  "After changing one annotation to two different places, the PlaceLinkService" should {
    
    "contain 3 correct place links" in {
      
      // TODO are there three?
      
      // TODO are they correct?
      
      failure
    }
    
  }

  "When searching for 'Vindobona', the PlaceLinkService" should {
    
    "retrieve only the place that has a link, not the one without a link" in {
      
      // TODO test has_child query 
      
      failure
    }
    
    "not return any places if the search is restricted by document ID" in {
      
      // TODO test has_child query
      
      failure
    }
    
  }
  
  "Deleting a parent place" should {
    
    "be possible without losing the link" in {
      
      // TODO delete place via PlaceService
      
      failure
    }
    
  }
  
  "After deleting the annotations, the PlaceLinkService" should {
    
    "contain no links" in {
      
      // TODO zero links?
      
      failure
    }
    
  }

}