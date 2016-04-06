package models.annotation

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import models.place.{ PlaceLink, PlaceLinkService }
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import storage.ES

object AnnotationService {

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
      linksCreated <- if (annotationCreated) PlaceLinkService.insertOrUpdatePlaceLinksForAnnotation(annotation) else Future.successful(false)
    } yield (linksCreated, version)    
  }

  def insertOrUpdateAnnotations(annotations: Seq[Annotation])(implicit context: ExecutionContext) =
    Future.sequence(for (result <- annotations.map(insertOrUpdateAnnotation(_))) yield result) 
  
  def findByDocId(id: String)(implicit context: ExecutionContext): Future[Seq[(Annotation, Long)]] = {
    ES.client execute {
      search in ES.IDX_RECOGITO / ANNOTATION query nestedQuery("annotates").query(termQuery("annotates.document" -> id)) limit 1000
    } map(_.as[(Annotation, Long)].toSeq)
  }
  
  def findByFilepartId(id: Int)(implicit context: ExecutionContext): Future[Seq[(Annotation, Long)]] = {
    ES.client execute {
      search in ES.IDX_RECOGITO / ANNOTATION query nestedQuery("annotates").query(termQuery("annotates.filepart" -> id)) limit 1000
    } map(_.as[(Annotation, Long)].toSeq)
  }

}
