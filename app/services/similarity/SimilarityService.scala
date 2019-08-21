package services.similarity

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import services.document.DocumentService
import services.generated.Tables.SIMILARITY
import services.generated.tables.records.SimilarityRecord
import storage.db.DB

/** A helper to work with SimilarityRecords more easily **/
case class Similarity(docId: String, jaroWinkler: Double, jaccard: Double)

@Singleton
class SimilarityService @Inject() (val db: DB, val documents: DocumentService, implicit val ctx: ExecutionContext) {

  private def fetchSimilarityMetrics(docId: String, n: Int = 10) = db.query { sql =>
    val query = 
      s"""
       SELECT
         similarity.*,
        (title_jaro_winkler + entity_jaccard) AS sort_score
       FROM similarity 
       JOIN document doc_a
         ON doc_a.id = doc_id_a
       JOIN document doc_b
         ON doc_b.id = doc_id_b
       WHERE doc_id_a = ? OR doc_id_b = ? 
         AND doc_a.public_visibility = 'PUBLIC'
         AND doc_b.public_visibility = 'PUBLIC'
       ORDER BY sort_score DESC
       LIMIT ${n}
       """

    sql.resultQuery(query, docId, docId).fetchArray.map { row => 
      val r = row.into(classOf[SimilarityRecord])
      if (r.getDocIdA == docId)
        Similarity(r.getDocIdB, r.getTitleJaroWinkler, r.getEntityJaccard)
      else 
        Similarity(r.getDocIdA, r.getTitleJaroWinkler, r.getEntityJaccard)
    }
  }

  /** Query the similarity metrics table for the top N most similar documents 
    *
    * Note that this could be rolled into one query (similarity, document metadata 
    * and access permissions). But things get really unreadable. So we split it into 
    * two queries (and re-use the document service for the second part.)
    */
  def findSimilar(docId: String, loggedInAs: Option[String], n: Int = 10) = {
    val f = for {
      similarities <- fetchSimilarityMetrics(docId, n)
      docsAndAccesslevels <- documents.getDocumentRecordsByIdWithAccessLevel(similarities.map(_.docId), loggedInAs)
    } yield (similarities, docsAndAccesslevels)

    f.map { case (similarities, docsAndAccessLevels) => 
      docsAndAccessLevels.map { case (doc, accesslevel) => 
        (doc, accesslevel, similarities.find(_.docId == doc.getId).get)
      } filter { _._2.canReadData } // Only expose docs with at least READ_DATA permission
    }
  }

}