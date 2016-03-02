package models

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import storage.ES
import play.api.libs.json.Json

object AnnotationService {

  private val ANNOTATION = "annotation"

  implicit object AnnotationIndexable extends Indexable  [Annotation] {
    override def json(a: Annotation): String = Json.stringify(Json.toJson(a))
  }
  
  def insertAnnotations(annotations: Seq[Annotation]) = {
    ES.client execute {
      bulk ( annotations.map(a =>  index into ES.IDX_RECOGITO / ANNOTATION source a) )
    }
  }

}
