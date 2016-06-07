package models.annotation

import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import storage.ES

trait AnnotationHistoryService extends HasAnnotationIndexing {
  
  private val ANNOTATION_HISTORY = "annotation_history"
    
  def insertVersion(annotation: Annotation)(implicit context: ExecutionContext): Future[Boolean] =
    ES.client execute {      
      index into ES.IDX_RECOGITO / ANNOTATION_HISTORY source annotation
    } map {
      _.isCreated
    } recover { case t: Throwable =>
      Logger.error("Error storing annotation version")
      Logger.error(t.toString)
      t.printStackTrace
      false
    }    
  
}