package services.document.read

import collection.JavaConversions._
import services.PublicAccess
import services.document.DocumentService
import services.generated.Tables.{DOCUMENT, SHARING_POLICY}
import storage.db.DB

/** For convenience: wraps public and shared documents count **/
case class AccessibleDocumentsCount(public: Long, shared: Option[Long]) {

  lazy val total = public + shared.getOrElse(0l)

}

/** Read-operations related to accessible/public/shared documents **/
trait AccessibleDocumentOps { self: DocumentService =>

  /** Lists users who have at least one document with visibility set to PUBLIC
    * 
    * Recogito uses this query to build the sitemap.txt file.
    */
  def listOwnersWithPublicDocuments(
    offset: Int = 0, limit: Int = 10000
  ) = db.query { sql =>
    sql.select(DOCUMENT.OWNER).from(DOCUMENT)
      .where(DOCUMENT.PUBLIC_VISIBILITY
        .equal(PublicAccess.PUBLIC.toString))
      .groupBy(DOCUMENT.OWNER)
      .limit(limit)
      .offset(offset)
      .fetch().into(classOf[String])
      .toSeq
  }

  /** Counts the total accessible documents that exist for the given owner. 
    * 
    * The result of this method depends on who's requesting the information. 
    * For anonymous visitors (accessibleTo = None), the count will cover public 
    * documents only. For a specific logged-in user, additional documents 
    * may be accessible, because they are shared with her/him.
    */
  def countAccessibleDocuments(
    owner: String, 
    accessibleTo: Option[String]
  ) = db.query { sql =>

    // Count all public documents
    val public =
      sql.selectCount()
         .from(DOCUMENT)
         .where(DOCUMENT.OWNER.equalIgnoreCase(owner)
           .and(DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString)))
         .fetchOne(0, classOf[Long])  

    // If there's a logged-in user, count shared
    val shared = accessibleTo.map { username => 
      sql.selectCount()
         .from(DOCUMENT)
         .where(DOCUMENT.OWNER.equalIgnoreCase(owner)
           // Don't double-count public docs!
           .and(DOCUMENT.PUBLIC_VISIBILITY.notEqual(PublicAccess.PUBLIC.toString))
           .and(DOCUMENT.ID.in(
             sql.select(SHARING_POLICY.DOCUMENT_ID)
                .from(SHARING_POLICY)
                .where(SHARING_POLICY.SHARED_WITH.equal(username))
             ))
         ).fetchOne(0, classOf[Long])
    }

    AccessibleDocumentsCount(public, shared)
  }

}