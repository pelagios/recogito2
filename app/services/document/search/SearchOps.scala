package services.document.search

import org.jooq.impl.DSL._
import services.{ContentType, Page, PublicAccess}
import services.document.read.results.MyDocument
import services.document.DocumentService
import services.generated.tables.records.DocumentRecord
import services.generated.Tables.{SHARING_POLICY, DOCUMENT}

trait SearchOps { self: DocumentService => 

  def searchAll(loggedInAs: Option[String], query: String) = db.query { sql => 
    val startTime = System.currentTimeMillis

    val q = loggedInAs match {

      case Some(username) => 
        // TODO include shared documents
        val q = 
          s"""
            SELECT 
              document.*,
              file_count,
              content_types
            FROM document
              LEFT JOIN (
                SELECT
                  count(*) AS file_count,
                  array_agg(DISTINCT content_type) AS content_types,
                  document_id
                FROM document_filepart
                GROUP BY document_id
              ) AS parts ON parts.document_id = document.id
            WHERE document.public_visibility = 'PUBLIC'
              AND lower(document.title) LIKE ?;
            """

        sql.resultQuery(q, s"%${query.toLowerCase}%")

      case None => 
        val q = 
          s"""
            SELECT 
              document.*,
              file_count,
              content_types
            FROM document
              LEFT JOIN (
                SELECT
                  count(*) AS file_count,
                  array_agg(DISTINCT content_type) AS content_types,
                  document_id
                FROM document_filepart
                GROUP BY document_id
              ) AS parts ON parts.document_id = document.id
            WHERE document.public_visibility = 'PUBLIC'
              AND lower(document.title) LIKE ?;
            """

        sql.resultQuery(q, s"%${query.toLowerCase}%")

    }

    val documents = q.fetchArray.map(MyDocument.build)
    Page(System.currentTimeMillis - startTime, documents.size, 0, documents.size, documents)
  }

  def searchMyDocuments(username: String, query: String) = db.query { sql =>
    sql.selectFrom(DOCUMENT)
       .where(DOCUMENT.OWNER.equal(username))
         .and(lower(DOCUMENT.TITLE).like(s"%${query.toLowerCase}%"))
       .fetchArray
       .toSeq
  }

  def searchSharedWithMe(username: String, query: String) = db.query { sql =>
    sql.select().from(SHARING_POLICY)
       .join(DOCUMENT)
         .on(DOCUMENT.ID.equal(SHARING_POLICY.DOCUMENT_ID))
       .where(SHARING_POLICY.SHARED_WITH.equal(username))
         .and(lower(DOCUMENT.TITLE).like(s"%${query.toLowerCase}%"))
       .fetchArray.map(_.into(classOf[DocumentRecord]))
       .toSeq
  }

}