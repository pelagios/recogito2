package services.entity.removal

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.RichSearchResponse
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import storage.es.ES

/** Implements delete functionality for entity records.
  *
  * Due to our union index concept, entity record removal is somewhat of
  * a complex process. Therefore we split this out into a separate source
  * file.
  */
class EntityServicerRemoveImpl(
  implicit val ctx: ExecutionContext,
  implicit val es: ES
) {
  
  private def deleteBatch(response: RichSearchResponse, cursor: Long = 0l): Future[Boolean] = ???
  
  def deleteByAuthoritySource(source: String): Future[Boolean] =
    es.client execute {
      search(ES.RECOGITO / ES.ENTITY) query {
        termQuery("is_conflation_of.source_authority" -> source)
      } limit 200 scroll "5m"
    } flatMap { deleteBatch(_) } map { success =>
      if (success) Logger.info(s"Successfully removed records from ${source}")
      else play.api.Logger.info(s"Delete process stopped. Something went wrong while removing records from ${source}")
      success
    }  
    
}