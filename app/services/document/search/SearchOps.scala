package services.document.search

import org.jooq.impl.DSL._
import services.{ContentType, Page, PublicAccess}
import services.document.read.results.MyDocument
import services.document.DocumentService
import services.generated.tables.records.DocumentRecord
import services.generated.Tables.{SHARING_POLICY, DOCUMENT}

trait SearchOps { self: DocumentService => 

  def search(loggedInAs: Option[String], args: SearchArgs) = db.query { sql =>
    val startTime = System.currentTimeMillis

    val baseSelect =
      """
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
      """

    val baseQuery = (loggedInAs, args.searchIn) match {
      case (None, _) => // No matter what 'searchIn' says, anonymous visitors only get public docs
        s"""
         ${baseSelect}
         WHERE document.public_visibility = 'PUBLIC' ;
         """

      case (Some(username), Scope.ALL) => 
        s"""
         $baseSelect
           LEFT OUTER JOIN sharing_policy
             ON sharing_policy.document_id = document.id
         WHERE document.owner = '$username'
           OR document.public_visibility = 'PUBLIC'
           OR sharing_policy.shared_with = '$username' ;
         """

      case (Some(username), Scope.MY) =>
        s"""
         $baseSelect
         WHERE document.owner = '$username' ;
         """

      case (Some(username), Scope.SHARED) =>
        s"""
         ${baseSelect}
           JOIN sharing_policy
             ON sharing_policy.document_id = document.id
         WHERE sharing_policy.shared_with = '$username' ;
         """
    }

    // TODO just a hack for now
    val documents = sql.resultQuery(baseQuery).fetchArray.map(MyDocument.build)
    Page(System.currentTimeMillis - startTime, documents.size, 0, documents.size, documents)
  }

  /*
  def oldSearch(loggedInAs: Option[String], args: SearchArgs) = db.query { sql => 
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

        sql.resultQuery(q, s"%${args.query.get.toLowerCase}%")

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

        sql.resultQuery(q, s"%${args.query.get.toLowerCase}%")

    }

    val documents = q.fetchArray.map(MyDocument.build)
    Page(System.currentTimeMillis - startTime, documents.size, 0, documents.size, documents)
  }
  */

}