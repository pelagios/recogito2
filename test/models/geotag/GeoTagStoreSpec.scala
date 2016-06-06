package models.geotag

import java.io.File
import java.util.UUID
import models.ContentType
import models.annotation._
import models.place.{ GazetteerUtils, PlaceService, ESPlaceStore }
import org.apache.commons.io.FileUtils
import org.joda.time.{ DateTime, DateTimeZone }
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.AfterAll
import org.junit.runner._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.Await
import scala.concurrent.duration._
import storage.ES

// So we can instantiate an ES Place + GeoTagStore
class TestGeoTagStore extends ESPlaceStore with ESGeoTagStore  

@RunWith(classOf[JUnitRunner])
class GeoTagStoreSpec extends Specification with AfterAll {
  
  sequential 

  override def afterAll = FileUtils.deleteDirectory(new File(TMP_IDX_DIR))
  
  private val TMP_IDX_DIR = "test/resources/models/place/tmp-idx"
  
  val now = DateTime.now().withMillisOfSecond(0).withZone(DateTimeZone.UTC)
  
  val annotatesBarcelona = Annotation(
    UUID.fromString("2fabe353-d517-4f18-b6a9-c9ec368b160a"),
    UUID.fromString("74de3052-7087-41b3-84cd-cb8f4a1caa79"),
    AnnotatedObject("hcylkmacy4xgkb", 1, ContentType.TEXT_PLAIN),
    None,
    Seq.empty[String],
    "char-offset:12",
    None,
    now,
    Seq(AnnotationBody(
      AnnotationBody.PLACE,
      None,
      now,
      None,
      Some("http://pleiades.stoa.org/places/246343"),
      Some(AnnotationStatus(AnnotationStatus.UNVERIFIED, None, now)))))
    
  val annotatesLancaster = Annotation(
    UUID.fromString("7cfa1504-26de-45ef-a590-8b60ea8a60e8"),
    UUID.fromString("e868423f-5ea9-42ed-bb7d-5e1fac9195a0"),
    AnnotatedObject("hcylkmacy4xgkb", 1, ContentType.TEXT_PLAIN),
    None,
    Seq.empty[String],
    "char-offset:124",
    None,
    now,
    Seq(AnnotationBody(
      AnnotationBody.PLACE,
      None,
      now,
      None,
      Some("http://pleiades.stoa.org/places/89222"),
      Some(AnnotationStatus(AnnotationStatus.UNVERIFIED, None, now)))))
    
  val annotatesVindobonaAndThessaloniki = Annotation(
    annotatesLancaster.annotationId,
    UUID.fromString("8b057d2f-65fe-465b-a636-50648066d678"),
    annotatesLancaster.annotates,
    annotatesLancaster.hasPreviousVersions,
    Seq("rainer"),
    annotatesLancaster.anchor,
    Some("rainer"),
    now.plusMinutes(10),
    Seq(
      AnnotationBody(
        AnnotationBody.PLACE,
        Some("rainer"),
        now.plusMinutes(10),
        None,
        Some("http://pleiades.stoa.org/places/128537"),
        Some(AnnotationStatus(AnnotationStatus.UNVERIFIED, None, now))),
      AnnotationBody(
        AnnotationBody.PLACE,
        Some("rainer"),
        now.plusMinutes(10),
        None,
        Some("http://pleiades.stoa.org/places/491741"),
        Some(AnnotationStatus(AnnotationStatus.UNVERIFIED, None, now)))))
  
  running (FakeApplication(additionalConfiguration = Map("recogito.index.dir" -> TMP_IDX_DIR))) {
    
    val linkToBarcelona = GeoTag(
      "http://dare.ht.lu.se/places/6534",
      annotatesBarcelona.annotationId,
      annotatesBarcelona.annotates.documentId,
      annotatesBarcelona.annotates.filepartId,
      "http://pleiades.stoa.org/places/246343")
    
    val linkToLancaster = GeoTag(
        "http://dare.ht.lu.se/places/23712",
        annotatesLancaster.annotationId,
        annotatesLancaster.annotates.documentId,
        annotatesLancaster.annotates.filepartId,
        "http://pleiades.stoa.org/places/89222")
        
    val linkToVindobona = GeoTag(
        "http://pleiades.stoa.org/places/128537",
        annotatesVindobonaAndThessaloniki.annotationId,
        annotatesVindobonaAndThessaloniki.annotates.documentId,
        annotatesVindobonaAndThessaloniki.annotates.filepartId,
        "http://pleiades.stoa.org/places/128537")
        
    val linkToThessaloniki = GeoTag(
        "http://dare.ht.lu.se/places/17068",
        annotatesVindobonaAndThessaloniki.annotationId,
        annotatesVindobonaAndThessaloniki.annotates.documentId,
        annotatesVindobonaAndThessaloniki.annotates.filepartId,
        "http://pleiades.stoa.org/places/491741")
        
    val testStore = new TestGeoTagStore() // Store extends GeoTagServiceLike
              
    def flush() = Await.result(ES.flushIndex, 10 seconds)
    def insertAnnotation(a: Annotation) = Await.result(AnnotationService.insertOrUpdateAnnotation(a), 10 seconds)
    def totalGeoTags() = Await.result(testStore.totalGeoTags(), 10 seconds)
    def findByAnnotationId(id: UUID) = Await.result(testStore.findGeoTagsByAnnotation(id), 10 seconds)
    def searchPlacesInDocument(query: String, documentId: String) = Await.result(testStore.searchPlacesInDocument(query, documentId), 10 seconds)
    
    "After creating 2 annotations with 1 geotag each, the GeoTagService" should {
      
      "contain 2 correct geotags" in {  
        Await.result(PlaceService.importRecords(GazetteerUtils.loadRDF(new File("test/resources/models/place/gazetteer_sample_dare.ttl"), "gazetteer_sample_dare.ttl")), 10 seconds)
        flush()
      
        Await.result(PlaceService.importRecords(GazetteerUtils.loadRDF(new File( "test/resources/models/place/gazetteer_sample_pleiades.ttl"), "gazetteer_sample_pleiades.ttl")), 10 seconds)
        flush()
      
        val (successInsertBarcelona, _) = insertAnnotation(annotatesBarcelona)
        val (successInsertLancaster, _) = insertAnnotation(annotatesLancaster)
        flush()

        successInsertBarcelona must equalTo(true)
        successInsertLancaster must equalTo(true)
        totalGeoTags() must equalTo(2)
      }
      
      "return the geotags by annotation ID" in {
        findByAnnotationId(annotatesBarcelona.annotationId) must equalTo(Seq(linkToBarcelona))
        findByAnnotationId(annotatesLancaster.annotationId) must equalTo(Seq(linkToLancaster))
      }
      
    }
    
    "After changing one annotation to two different places, the GeoTagService" should {
      
      "contain 3 correct geotags" in {
        val (success, _) = insertAnnotation(annotatesVindobonaAndThessaloniki)
        flush()
        
        success must equalTo(true)
        totalGeoTags() must equalTo(3)
      }
      
      "return the geotags by annotation ID" in {
        findByAnnotationId(annotatesBarcelona.annotationId) must equalTo(Seq(linkToBarcelona))
        findByAnnotationId(annotatesVindobonaAndThessaloniki.annotationId) must containAllOf(Seq(linkToThessaloniki, linkToVindobona))
      }
      
    }
  
    "When searching for 'Vindobona', the GeoTagService" should {
      
      "retrieve only the Vindobona linked to the test document" in {
        val places = searchPlacesInDocument("vindobona", annotatesVindobonaAndThessaloniki.annotates.documentId)
        places.total must equalTo(1)
        places.items.head._1.id must equalTo("http://pleiades.stoa.org/places/128537")
      }
      
      "not return any places if the search is restricted to another document ID" in {
        val places = searchPlacesInDocument("vindobona", "not-a-document-id")
        places.total must equalTo(0)
      }
      
    }
    
    "Deleting a parent place" should {
      
      "be possible without losing the geotag" in {
        // That's hacky, but works they way we've set things up currently    
        // In any case - deleting a place is something that only happens underneath the hood,
        // so we don't want to expose this as a functionality in the PlaceService

        val deleteSuccess = Await.result(testStore.deletePlace("http://pleiades.stoa.org/places/128537"), 10 seconds)
        deleteSuccess must equalTo(true)
        flush()
        
        totalGeoTags() must equalTo(3)
        
        val totalPlaces = Await.result(PlaceService.totalPlaces(), 10 seconds)
        totalPlaces must equalTo(4)
        
      }
      
    }
    
    "After deleting the annotations, the GeoTagService" should {
      
      "contain no geotags" in {
        val success = 
          Seq(annotatesBarcelona.annotationId,
            annotatesVindobonaAndThessaloniki.annotationId).map { annotationId => 
              Await.result(AnnotationService.deleteAnnotation(annotationId), 10 seconds)  
          }
        
        flush()
        
        success.filter(_ == true).size must equalTo(2)
        totalGeoTags() must equalTo(0)
      }
      
    }
  
  }

}