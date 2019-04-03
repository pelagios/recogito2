package services.document.read

import collection.JavaConversions._
import java.util.UUID
import scala.concurrent.Future
import services.PublicAccess
import services.document.DocumentService
import services.generated.Tables.{DOCUMENT, FOLDER_ASSOCIATION, SHARING_POLICY}
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

  private def listAccessibleIdsInRoot(owner: String, loggedInAs: Option[String]) = db.query { sql => 
    loggedInAs match {
      case Some(username) =>
        sql.select(DOCUMENT.ID)
          .from(DOCUMENT)
          .leftOuterJoin(FOLDER_ASSOCIATION)
            .on(FOLDER_ASSOCIATION.DOCUMENT_ID.equal(DOCUMENT.ID))
          .leftOuterJoin(SHARING_POLICY)
            .on(SHARING_POLICY.DOCUMENT_ID.equal(DOCUMENT.ID))
          .where(
            DOCUMENT.OWNER.equalIgnoreCase(owner)
              .and(FOLDER_ASSOCIATION.FOLDER_ID.isNull)
              .and(
                DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString)
                  .or(SHARING_POLICY.SHARED_WITH.equal(username))
              )
            )
          .fetch(0, classOf[String])
          .toSeq

      case None => 
        sql.select(DOCUMENT.ID)
          .from(DOCUMENT)
          .leftOuterJoin(FOLDER_ASSOCIATION)
            .on(FOLDER_ASSOCIATION.DOCUMENT_ID.equal(DOCUMENT.ID))
          .where(DOCUMENT.OWNER.equalIgnoreCase(owner)
            .and(DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString))
            .and(FOLDER_ASSOCIATION.FOLDER_ID.isNull))
          .fetch(0, classOf[String])
          .toSeq
    }
  }

  private def listAccessibleIdsInFolder(folder: UUID, loggedInAs: Option[String]) = db.query { sql => 
    loggedInAs match {
      case Some(username) => 
        sql.select(DOCUMENT.ID)
          .from(DOCUMENT)
          .leftOuterJoin(FOLDER_ASSOCIATION)
            .on(FOLDER_ASSOCIATION.DOCUMENT_ID.equal(DOCUMENT.ID))
          .leftOuterJoin(SHARING_POLICY)
            .on(SHARING_POLICY.DOCUMENT_ID.equal(DOCUMENT.ID))
          .where(FOLDER_ASSOCIATION.FOLDER_ID.equal(folder)
            .and(
              DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString)
                .or(SHARING_POLICY.SHARED_WITH.equal(username))
            ))
          .fetch(0, classOf[String])
          .toSeq
      
      case None => 
        sql.select(DOCUMENT.ID)
          .from(DOCUMENT)
          .leftJoin(FOLDER_ASSOCIATION)
            .on(FOLDER_ASSOCIATION.DOCUMENT_ID.equal(DOCUMENT.ID))
          .where(DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString)
            .and(FOLDER_ASSOCIATION.FOLDER_ID.equal(folder)))
          .fetch(0, classOf[String])
          .toSeq
    }
  }

  /** Delegate to the appropriate private method, based on folder value **/
  def listAccessibleIds(
    owner: String, 
    folder: Option[UUID], 
    loggedInAs: Option[String]
  ): Future[Seq[String]] = 
    folder match {
      case Some(folderId) => listAccessibleIdsInFolder(folderId, loggedInAs)
      case None => listAccessibleIdsInRoot(owner, loggedInAs)
    }

  /** Counts the total accessible documents that exist for the given owner. 
    * 
    * The result of this method depends on who's requesting the information. 
    * For anonymous visitors (accessibleTo = None), the count will cover public 
    * documents only. For a specific logged-in user, additional documents 
    * may be accessible, because they are shared with her/him.
    */
  def countAllAccessibleDocuments(
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