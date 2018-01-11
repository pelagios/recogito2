package services.geotag

import java.io.File
import java.util.UUID
import services.ContentType
import services.annotation._
import services.place._
import services.place.crosswalks.PelagiosRDFCrosswalk
import org.apache.commons.io.FileUtils
import org.joda.time.{DateTime, DateTimeZone}
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.AfterAll
import org.junit.runner._
import play.api.Play
import play.api.test._
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import storage.ES
import storage.HasES

@RunWith(classOf[JUnitRunner])
class GeoTagStoreSpec extends Specification with AfterAll {
  
  sequential
  
  override def afterAll = FileUtils.deleteDirectory(new File(TMP_IDX_DIR))
  
  private val TMP_IDX_DIR = "test/resources/services/place/tmp-idx"
  
  val now = DateTime.now().withMillisOfSecond(0).withZone(DateTimeZone.UTC)
  
  val annotatesBarcelona = Annotation(
    UUID.fromString("2fabe353-d517-4f18-b6a9-c9ec368b160a"),
    UUID.fromString("74de3052-7087-41b3-84cd-cb8f4a1caa79"),
    AnnotatedObject("hcylkmacy4xgkb", UUID.fromString("d8e2c22f-e5c0-4360-85bd-f5e921bc30dc"), ContentType.TEXT_PLAIN),
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
      None,
      Some(AnnotationStatus(AnnotationStatus.UNVERIFIED, None, now)))))
    
  val annotatesLancaster = Annotation(
    UUID.fromString("7cfa1504-26de-45ef-a590-8b60ea8a60e8"),
    UUID.fromString("e868423f-5ea9-42ed-bb7d-5e1fac9195a0"),
    AnnotatedObject("hcylkmacy4xgkb", UUID.fromString("d8e2c22f-e5c0-4360-85bd-f5e921bc30dc"), ContentType.TEXT_PLAIN),
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
      None,
      Some(AnnotationStatus(AnnotationStatus.UNVERIFIED, None, now)))))
    
  val annotatesVindobonaAndThessaloniki = Annotation(
    annotatesLancaster.annotationId,
    UUID.fromString("8b057d2f-65fe-465b-a636-50648066d678"),
    annotatesLancaster.annotates,
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
        None,
        Some(AnnotationStatus(AnnotationStatus.UNVERIFIED, None, now))),
      AnnotationBody(
        AnnotationBody.PLACE,
        Some("rainer"),
        now.plusMinutes(10),
        None,
        Some("http://pleiades.stoa.org/places/491741"),
        None,
        Some(AnnotationStatus(AnnotationStatus.UNVERIFIED, None, now)))))
        
    val application = GuiceApplicationBuilder().configure("recogito.index.dir" -> TMP_IDX_DIR).build()
    implicit val executionContext = application.injector.instanceOf[ExecutionContext]
        
    val es = application.injector.instanceOf(classOf[ES])
    val annotations = application.injector.instanceOf(classOf[AnnotationService])
    val places = application.injector.instanceOf(classOf[PlaceService])
    
    val linkToBarcelona = GeoTag(
      annotatesBarcelona.annotationId,
      annotatesBarcelona.annotates.documentId,
      annotatesBarcelona.annotates.filepartId,
      "http://pleiades.stoa.org/places/246343",
      Seq.empty[String], Seq.empty[String], None,
      annotatesBarcelona.lastModifiedAt)
    
    val linkToLancaster = GeoTag(
      annotatesLancaster.annotationId,
      annotatesLancaster.annotates.documentId,
      annotatesLancaster.annotates.filepartId,
      "http://pleiades.stoa.org/places/89222",
      Seq.empty[String], Seq.empty[String], None,
      annotatesLancaster.lastModifiedAt)
        
    val linkToVindobona = GeoTag(
      annotatesVindobonaAndThessaloniki.annotationId,
      annotatesVindobonaAndThessaloniki.annotates.documentId,
      annotatesVindobonaAndThessaloniki.annotates.filepartId,
      "http://pleiades.stoa.org/places/128537",
      Seq.empty[String], Seq("rainer"), Some("rainer"),
      annotatesVindobonaAndThessaloniki.lastModifiedAt)
        
    val linkToThessaloniki = GeoTag(
      annotatesVindobonaAndThessaloniki.annotationId,
      annotatesVindobonaAndThessaloniki.annotates.documentId,
      annotatesVindobonaAndThessaloniki.annotates.filepartId,
      "http://pleiades.stoa.org/places/491741",
      Seq.empty[String], Seq("rainer"), Some("rainer"),
      annotatesVindobonaAndThessaloniki.lastModifiedAt)
              
    def flush() = Await.result(es.flush, 10 seconds)
    def insertAnnotation(a: Annotation) = Await.result(annotations.insertOrUpdateAnnotation(a), 10 seconds)
    def totalGeoTags() = Await.result(places.totalGeoTags(), 10 seconds)
    def findByAnnotationId(id: UUID) = Await.result(places.findGeoTagsByAnnotation(id), 10 seconds)
    def searchPlacesInDocument(query: String, documentId: String) = Await.result(places.searchPlacesInDocument(query, documentId), 10 seconds)
    
    "After creating 2 annotations with 1 geotag each, the GeoTagService" should {
      
      "contain 2 correct geotags" in {  
        Await.result(places.importRecords(PelagiosRDFCrosswalk.readFile(new File("test/resources/services/place/gazetteer_sample_dare.ttl"))), 10 seconds)
        flush()
      
        Await.result(places.importRecords(PelagiosRDFCrosswalk.readFile(new File( "test/resources/services/place/gazetteer_sample_pleiades.ttl"))), 10 seconds)
        flush()
      
        val (successInsertBarcelona, _, _) = insertAnnotation(annotatesBarcelona)
        val (successInsertLancaster, _, _) = insertAnnotation(annotatesLancaster)
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
        val (success, _, _) = insertAnnotation(annotatesVindobonaAndThessaloniki)
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

        val deleteSuccess = Await.result(places.deletePlace("http://pleiades.stoa.org/places/128537"), 10 seconds)
        deleteSuccess must equalTo(true)
        flush()
        
        totalGeoTags() must equalTo(3)
        
        val totalPlaces = Await.result(places.totalPlaces(), 10 seconds)
        totalPlaces must equalTo(4)
        
      }
      
    }
    
    "After deleting the annotations, the GeoTagService" should {
      
      "contain no geotags" in {
        val success = 
          Seq(annotatesBarcelona.annotationId,
            annotatesVindobonaAndThessaloniki.annotationId).flatMap { annotationId => 
              Await.result(annotations.deleteAnnotation(annotationId, "rainer", DateTime.now), 10 seconds)  
          }
        
        flush()
        
        success.size must equalTo(2)
        totalGeoTags() must equalTo(0)
      }
      
    }
    
    "When merging places, the PlaceStore" should {

      val recordA = GazetteerRecord(
        "http://www.example.com/places/a",
        Gazetteer("Example Gazetteer"),
        now,
        None,
        "Record A",
        Seq(),
        Seq(),
        None,
        None,
        None,
        Seq(),
        None,
        None,
        Seq(),
        Seq())
      
      val recordB = recordA.copy(
        uri = "http://www.example.com/places/b",
        title = "Record B"
      )
      
      val recordC = recordA.copy(
        uri = "http://www.example.com/places/c",
        title = "Record C",
        closeMatches = Seq("http://www.example.com/places/a", "http://www.example.com/places/b")
      )
      
      val annotation = Annotation(
        UUID.randomUUID,
        UUID.randomUUID,
        AnnotatedObject("cbrjoouex3qyu3", UUID.randomUUID, ContentType.TEXT_PLAIN),
        Seq.empty[String],
        "char-offset:1234",
        None,
        now,
        Seq(AnnotationBody(
          AnnotationBody.PLACE,
          None,
          now,
          None,
          Some("http://www.example.com/places/b"),
          None,
          Some(AnnotationStatus(AnnotationStatus.UNVERIFIED, None, now)))))

      "update the GeoTag-Place relation accordingly" in {
        // Import records A and B as separate places
        Await.result(places.importRecords(Seq(recordA, recordB)), 10 seconds)
        flush()
        
        // Import annotation pointing to record B
        insertAnnotation(annotation)
        flush()
        
        // If the GeoTag was created properly, we'll find a single place, consisting of just recordB
        val placesInDocumentBefore = Await.result(places.listPlacesInDocument(annotation.annotates.documentId), 10 seconds)
        placesInDocumentBefore.total must equalTo(1)
        placesInDocumentBefore.items.head._1.isConflationOf must equalTo(Seq(recordB))
        
        // Now import record C, which will merge records A and B to one place
        Await.result(places.importRecord(recordC), 10 seconds)
        flush()
        
        // If GeoTags were properly re-written, the query will return the merged place
        val placesInDocumentAfter = Await.result(places.listPlacesInDocument(annotation.annotates.documentId), 10 seconds)
        placesInDocumentAfter.total must equalTo(1)
        placesInDocumentAfter.items.head._1.isConflationOf must containAllOf(Seq(recordA, recordB, recordC))
      }
      
    }
  
//  }

}