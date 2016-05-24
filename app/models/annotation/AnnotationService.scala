package models.annotation

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import java.util.UUID
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import storage.ES
import models.geotag.ESGeoTagStore

object AnnotationService extends ESGeoTagStore {

  private val ANNOTATION = "annotation"

  implicit object AnnotationIndexable extends Indexable[Annotation] {
    override def json(a: Annotation): String = Json.stringify(Json.toJson(a))
  }

  implicit object AnnotationHitAs extends HitAs[(Annotation, Long)] {
    override def as(hit: RichSearchHit): (Annotation, Long) =
      (Json.fromJson[Annotation](Json.parse(hit.sourceAsString)).get, hit.version)
  }
  
  def insertOrUpdateAnnotation(annotation: Annotation)(implicit context: ExecutionContext): Future[(Boolean, Long)] = {
    def upsertAnnotation(a: Annotation): Future[(Boolean, Long)] = {
      ES.client execute {
        update id a.annotationId in ES.IDX_RECOGITO / ANNOTATION source a docAsUpsert
      } map { r => 
        (true, r.getVersion)
      } recover { case t: Throwable =>
        Logger.error("Error indexing annotation " + annotation.annotationId + ": " + t.getMessage)
        t.printStackTrace
        (false, -1l)
      }
    }
    
    for {
      (annotationCreated, version) <- upsertAnnotation(annotation)
      linksCreated <- if (annotationCreated) insertOrUpdateGeoTagsForAnnotation(annotation) else Future.successful(false)
    } yield (linksCreated, version)    
  }

  // TODO 
  // * perform sequentially, using a proper foldLeft + map construct
  // * generate proper success/failed result, with list of failed annotations
  // * perform retries with failed imports (cf. place import)
  def insertOrUpdateAnnotations(annotations: Seq[Annotation])(implicit context: ExecutionContext) =
    Future.sequence(for (result <- annotations.map(insertOrUpdateAnnotation(_))) yield result) 
  
  def findById(annotationId: UUID)(implicit context: ExecutionContext): Future[Option[(Annotation, Long)]] = {
    ES.client execute {
      get id annotationId.toString from ES.IDX_RECOGITO / ANNOTATION 
    } map { response =>
      if (response.isExists) {
        val source = Json.parse(response.getSourceAsString)
        Some((Json.fromJson[Annotation](source).get, response.getVersion))    
      } else {
        None
      }
    }
  }
    
  def deleteAnnotation(annotationId: UUID)(implicit context: ExecutionContext): Future[Boolean] =
    ES.client execute {
      delete id annotationId.toString from ES.IDX_RECOGITO / ANNOTATION
    } flatMap { response =>
      if (response.isFound)
        deleteGeoTagsByAnnotation(annotationId)
      else
        Future.successful(false)
    } recover { case t: Throwable =>
      t.printStackTrace()
      false
    }    
  
  /*
    findGeoTagsByAnnotationWithId(annotationId.toString).flatMap { idsAndLinks =>
      if (idsAndLinks.size > 0) {
        ES.client execute {
          bulk ( idsAndLinks.map { case (linkId, _) => delete id linkId from ES.IDX_RECOGITO / GEOTAG } )
        } map {
          !_.hasFailures
        } recover { case t: Throwable =>
          t.printStackTrace()
          false
        }
      } else {
        // Nothing to delete
        Future.successful(true)
      }
    }
   */
    
    
  def findByDocId(id: String)(implicit context: ExecutionContext): Future[Seq[(Annotation, Long)]] = {
    ES.client execute {
      search in ES.IDX_RECOGITO / ANNOTATION query nestedQuery("annotates").query(termQuery("annotates.document" -> id)) limit 1000
    } map(_.as[(Annotation, Long)].toSeq)
  }
  
  /** Unfortunately, ElasticSearch doesn't support delete-by-query directly, so this is a two-step-process **/
  def deleteByDocId(docId: String)(implicit context: ExecutionContext): Future[Boolean] =
    findByDocId(docId).flatMap { annotationsAndVersions =>
      if (annotationsAndVersions.size > 0) {
        ES.client execute {
          bulk ( annotationsAndVersions.map { case (annotation, _) => delete id annotation.annotationId from ES.IDX_RECOGITO / ANNOTATION } )
        } map {
          !_.hasFailures
        } recover { case t: Throwable =>
          t.printStackTrace()
          false
        }
      } else {
        // Nothing to delete
        Future.successful(true)
      }
    }
  
  def findByFilepartId(id: Int)(implicit context: ExecutionContext): Future[Seq[(Annotation, Long)]] = {
    ES.client execute {
      search in ES.IDX_RECOGITO / ANNOTATION query nestedQuery("annotates").query(termQuery("annotates.filepart" -> id)) limit 1000
    } map(_.as[(Annotation, Long)].toSeq)
  }

}
