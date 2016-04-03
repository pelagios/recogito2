package models.place

import java.io.File
import org.apache.commons.io.FileUtils
import org.joda.time.DateTime
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.AfterAll
import org.junit.runner._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.Await
import scala.concurrent.duration._
import storage.ES
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class PlaceStoreSpec extends Specification with AfterAll {
  
  // Force Specs2 to execute tests in sequential order
  sequential 
  
  val TMP_IDX_DIR = "test/resources/tmp-idx"
  
  override def afterAll =
    FileUtils.deleteDirectory(new File(TMP_IDX_DIR))
    
  def createNewTestPlace() = {
    val record = GazetteerRecord(
      "http://www.example.com/record",
      Gazetteer("DummyGazetteer"),
      DateTime.now(),
      "Record " + Random.nextInt(),
      Seq.empty[String],
      Seq.empty[Description],
      Seq(Name("Dummy")),
      None,
      None,
      None,
      Seq("http://www.example.com/match"),
      Seq.empty[String])
      
    Place(record.uri, record.title, None, None, None, Seq(record))
  }
    
  val store = new ESPlaceStore()
  
  val initialPlace = createNewTestPlace()
  
  def flush() = Await.result(ES.flushIndex, 10 seconds)
  
  def totalPlaces() = Await.result(store.totalPlaces(), 10 seconds)
  
  def updatePlace(place: Place) = Await.result(store.insertOrUpdatePlace(place), 10 seconds)
    
  def findByURI(uri: String) = Await.result(store.findByURI(uri), 10 seconds)
  
  def findByPlaceOrMatchURIs(uri: String) = Await.result(store.findByPlaceOrMatchURIs(Seq(uri)), 10 seconds)
  
  def findByName(name: String) = Await.result(store.searchByName(name), 10 seconds)
  
  running (FakeApplication(additionalConfiguration = Map("recogito.index.dir" -> TMP_IDX_DIR))) {
    
    "The ESPlaceStore" should {
      
      "properly import the test record" in {
        val (success, _) = updatePlace(initialPlace)
        
        // In this case we just force a flush - so we can get on with the test!
        flush()

        success must equalTo(true)
      }
      
      "contain one place total" in {
        totalPlaces() must equalTo(1)
      }
      
      "return the test place by record URI" in {
        val tuple = findByURI("http://www.example.com/record")
        tuple.isDefined must equalTo(true)
        tuple.get._1 must equalTo(initialPlace)
      }
      
      "return the test place by closeMatch URI" in {
        val result = findByPlaceOrMatchURIs("http://www.example.com/match")
        result.size must equalTo(1)
        result.head._1 must equalTo(initialPlace)
      }
      
      "return the test place by name" in {
        val result = findByName(initialPlace.names.head._1.name)
        result.size must equalTo(1)
        result.head._1 must equalTo(initialPlace)
      }
      
      "increase version numbers on subsequent updates" in {
        val versions = (1 to 10).foldLeft(0l)((previousVersion, _) => {
          val (success, version) = updatePlace(createNewTestPlace())
          success must equalTo(true)
          version must greaterThan(previousVersion)
          version
        })
        success
      }
      
      "properly delete the test record" in {
        val success = Await.result(store.deletePlace(initialPlace.id), 10 seconds)
        
        flush()
        
        success must equalTo(true)
        
      }
      
    }
  
  }
    
}