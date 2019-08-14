package services.similarity

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import services.generated.Tables.SIMILARITY
import services.generated.tables.records.SimilarityRecord
import storage.db.DB

@Singleton
class SimilarityService @Inject() (val db: DB, implicit val ctx: ExecutionContext) {

  /** Query the similarity metrics table for the top N most similar documents **/
  def findSimilar(docId: String, n: Int = 10) = db.query { sql => 
    val query = 
      s"""
       SELECT
         *,
        (coalesce(title_jaro_winkler,0) + coalesce(entity_jaccard, 0)) AS sort_score
       FROM similarity 
       WHERE doc_id_a = ? OR doc_id_b = ? 
       ORDER BY sort_score DESC
       LIMIT ${n}
       """

    sql.resultQuery(query, docId, docId).fetchArray.map { record => 
      record.into(classOf[SimilarityRecord])
    }
  }

}