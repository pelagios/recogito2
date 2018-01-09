package models.place

import java.io.File
import models.geotag.ESGeoTagStore
import org.apache.commons.io.FileUtils
import org.joda.time.{ DateTime, DateTimeZone }
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
import storage.{ ES, HasES }
import scala.util.Random
import play.api.Play
import play.api.inject.guice.GuiceApplicationBuilder

// So we can instantiate an ElasticSearch place store
class TestPlaceStore(val es: ES) extends ESPlaceStore with HasES with ESGeoTagStore

@RunWith(classOf[JUnitRunner])
class PlaceStoreSpec extends Specification with AfterAll {
  
  // Force Specs2 to execute tests in sequential order
  sequential 
  
  val TMP_IDX_DIR = "test/resources/models/place/tmp-idx"
  
  override def afterAll =
    FileUtils.deleteDirectory(new File(TMP_IDX_DIR))
    
  def createNewTestPlace() = {
    val record = GazetteerRecord(
      "http://www.example.com/record",
      Gazetteer("DummyGazetteer"),
      DateTime.now().withMonthOfYear(1).withDayOfMonth(1).withTime(0, 0, 0, 0).withZone(DateTimeZone.UTC),
      None,
      "Record " + Random.nextInt(),
      Seq.empty[Description],
      Seq(Name("Dummy")),
      None,
      None,
      None,
      Seq.empty[String],
      None,
      None,
      Seq("http://www.example.com/match"),
      Seq.empty[String])
      
    Place(record.uri, None, None, None, Seq(record))
  }
  
  val application = GuiceApplicationBuilder().configure("recogito.index.dir" -> TMP_IDX_DIR).build()
 
  val es = application.injector.instanceOf(classOf[ES])
 
  val testStore = new TestPlaceStore(es)
  
  val initialPlace = createNewTestPlace()
  
  def flush() = Await.result(es.flushIndex, 10 seconds)
  
  def totalPlaces() = Await.result(testStore.totalPlaces(), 10 seconds)
  
  def updatePlace(place: Place) = Await.result(testStore.insertOrUpdatePlace(place), 10 seconds)
    
  def findByURI(uri: String) = Await.result(testStore.findByURI(uri), 10 seconds)
  
  def findByPlaceOrMatchURIs(uris: Seq[String]) = Await.result(testStore.findByPlaceOrMatchURIs(uris), 10 seconds)
  
  def findByName(name: String) = Await.result(testStore.searchPlaces(name), 10 seconds)

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
    
    "return the test place by a closeMatch URI" in {
      val result = findByPlaceOrMatchURIs(
          Seq("http://www.example.com/match",
              "http://www.example.com/noMatch1", 
              "http://www.example.com/noMatch2"))
              
      result.size must equalTo(1)
      result.head._1 must equalTo(initialPlace)
    }
    
    "return the test place by its URI using the findByPlaceOrMatchURIs method" in {
      val result = findByPlaceOrMatchURIs(
          Seq("http://www.example.com/noMatch1",
              "http://www.example.com/noMatch2", 
              "http://www.example.com/record"))
              
      result.size must equalTo(1)
      result.head._1 must equalTo(initialPlace)
    }
    
    "return the test place by name" in {
      val result = findByName(initialPlace.names.head._1.name)
      result.total must equalTo(1)
      result.items.size must equalTo(1)
      result.items.head._1 must equalTo(initialPlace)
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
      val success = Await.result(testStore.deletePlace(initialPlace.id), 10 seconds)
      
      flush()
      
      success must equalTo(true)
      
    }
    
  }
    
}