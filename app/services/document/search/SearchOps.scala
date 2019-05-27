package services.document.search

import org.jooq.impl.DSL._
import services.{ContentType, Page, PublicAccess}
import services.document.read.results.MyDocument
import services.document.DocumentService
import services.generated.tables.records.DocumentRecord
import services.generated.Tables.{SHARING_POLICY, DOCUMENT}

trait SearchOps { self: DocumentService => 

  private def render(q: String, vars: String*) = {
    val withBrackets = condition(q, vars:_*).toString
    // Bit of a hack... JOOQ wraps conditions in brackets and we need to get rid of them
    withBrackets.substring(1, withBrackets.size - 1)
  }

  /** Appends the query phrase, if any */
  private def setQueryPhrase(args: SearchArgs): String =>  String = query =>
    args.query match {
      case Some(phrase) => render(
        s"""
         $query 
           AND lower(document.title) LIKE ?
         """,
        s"%${phrase.toLowerCase}%")

      case None => query
    }

  /** Appends the scope filter part **/
  private def setScope(loggedInAs: Option[String], args: SearchArgs): String => String = query =>
    (loggedInAs, args.searchIn) match {
      case (None, _) => // No matter what 'searchIn' says, anonymous visitors only get public docs
        s"""
         $query
         WHERE document.public_visibility = 'PUBLIC'
         """

      case (Some(username), Scope.ALL) => render(
        s"""
         $query
           LEFT OUTER JOIN sharing_policy
             ON sharing_policy.document_id = document.id 
               AND sharing_policy.shared_with = ?
         WHERE (
           document.owner = ?
           OR document.public_visibility = 'PUBLIC'
           OR sharing_policy.shared_with = ?
         )
         """, username, username, username)

      case (Some(username), Scope.MY) => render(
        s"""
         $query
         WHERE document.owner = ?
         """, username)

      case (Some(username), Scope.SHARED) => render(
        s"""
         $query
           JOIN sharing_policy
             ON sharing_policy.document_id = document.id
         WHERE sharing_policy.shared_with = ?
         """, username)
    }

  private def setDocumentType(args: SearchArgs): String => String = query => {

    import DocumentType._

    args.documentType match {
      case Some(TEXT) =>
        s"""
         $query AND (
           'TEXT_PLAIN' = ANY(content_types) OR
           'TEXT_TEIXML' = ANY(content_types)
         )"""

      case Some(IMAGE) =>
        s"""
         $query AND (
           'IMAGE_UPLOAD' = ANY(content_types) OR
           'IMAGE_IIIF' = ANY(content_types)
         )"""

      case Some(TABLE) =>
        s"$query AND 'DATA_CSV' = ANY(content_types)"
        
      case  None => query
    }
  }

  private def setOwner(args: SearchArgs): String =>  String = query => args.owner match {
    case Some(username) => render(
      s"""
       $query AND document.owner = ?
       """, username)
    
    case None =>  query
  }

  def search(loggedInAs: Option[String], args: SearchArgs) = db.query { sql => 
    val startTime = System.currentTimeMillis

    // The base component for the DB query
    val base =
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

    val buildQuery = 
      setScope(loggedInAs, args) andThen 
      setDocumentType(args) andThen
      setOwner(args) andThen
      setQueryPhrase(args)

    // TODO just a hack for now
    val documents = sql.resultQuery(buildQuery(base)).fetchArray.map(MyDocument.build)
    Page(System.currentTimeMillis - startTime, documents.size, 0, documents.size, documents)
  }

}