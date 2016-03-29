package models.annotation

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import storage.ES

object AnnotationService {

  private val ANNOTATION = "annotation"

  implicit object AnnotationIndexable extends Indexable[Annotation] {
    override def json(a: Annotation): String = Json.stringify(Json.toJson(a))
  }

  implicit object AnnotationHitAs extends HitAs[Annotation] {
    override def as(hit: RichSearchHit): Annotation =
      Json.fromJson[Annotation](Json.parse(hit.sourceAsString)).get
  }

  def insertAnnotations(annotations: Seq[Annotation]) = {
    annotations.foreach(a => {
      ES.client execute { index into ES.IDX_RECOGITO / ANNOTATION source a }
    })
   }
  
  def findByDocId(id: String)(implicit context: ExecutionContext) = {
    ES.client execute {
      search in ES.IDX_RECOGITO / ANNOTATION query nestedQuery("annotates").query(termQuery("annotates.document" -> id)) limit 1000
    } map(_.as[Annotation].toSeq)
  }
  
  def findByFilepartId(id: Int)(implicit context: ExecutionContext) = {
    ES.client execute {
      search in ES.IDX_RECOGITO / ANNOTATION query nestedQuery("annotates").query(termQuery("annotates.filepart" -> id)) limit 1000
    } map(_.as[Annotation].toSeq)
  }

}
