package services.document.read

import collection.JavaConversions._
import play.api.Logger
import scala.concurrent.Future
import services.{ContentType, PublicAccess, RuntimeAccessLevel, SharingLevel}
import services.document.{DocumentService, ExtendedDocumentMetadata}
import services.document.read.results.MyDocument
import services.generated.Tables._
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord, SharingPolicyRecord, UserRecord}

/** Default Read operations on document records **/
trait DocumentReadOps { self: DocumentService =>

  /** Derives the runtime access level. 
    * 
    * The runtime access level is specific to who's accessing the document,
    * and can depend on the document's public visibility settings as  
    * well as any existing sharing policy the current user has on the 
    * document. 
    */
  private def determineAccessLevel(
    document: DocumentRecord, 
    sharingPolicies: Seq[SharingPolicyRecord],
    forUser: Option[String]
  ): RuntimeAccessLevel = {
    // Shorthand
    val isVisibleToPublic = 
      document.getPublicVisibility == PublicAccess.PUBLIC.toString ||
      document.getPublicVisibility == PublicAccess.WITH_LINK.toString
      
    // The accesslevel determined purely from the document's public access settings
    def getPublicAccessLevel(): RuntimeAccessLevel = PublicAccess.AccessLevel.withName(document.getPublicAccessLevel) match {
      case Some(PublicAccess.READ_DATA) => RuntimeAccessLevel.READ_DATA
      case Some(PublicAccess.READ_ALL) => RuntimeAccessLevel.READ_ALL
      case Some(PublicAccess.WRITE) =>
        forUser match {
          case Some(_) => // Write access to any logged-in user
            RuntimeAccessLevel.WRITE
          case None => // READ_ALL access to anonymous visitors
            RuntimeAccessLevel.READ_ALL
        }
      case None =>
        Logger.warn(s"Document ${document.getId} visible to public, but no access level set")
        RuntimeAccessLevel.FORBIDDEN      
    }
    
    forUser match {      
      // Trivial case: the user is the owner of the document
      case Some(user) if (document.getOwner == user) =>
        RuntimeAccessLevel.OWNER
      
      case Some(user) =>
        sharingPolicies.filter(_.getSharedWith == user).headOption.flatMap(p => SharingLevel.withName(p.getAccessLevel)) match {
          // There's a sharing policy for this user
          case Some(policy) => RuntimeAccessLevel.fromSharingLevel(policy)
          
          // No sharing policy, but document might still be public
          case None => 
            if (isVisibleToPublic) getPublicAccessLevel()
            else RuntimeAccessLevel.FORBIDDEN
        }

      // Anonymous access - the user is not logged in
      case None =>
        if (isVisibleToPublic) getPublicAccessLevel() 
        else RuntimeAccessLevel.FORBIDDEN
    }
  }
  
  /** Retrieves just the DocumentRecord by ID. 
    * 
    * Also includes runtime access permissions for the given user 
    * for convenience.
    */
  def getDocumentRecordById(
    id: String,
    loggedInUser: Option[String] = None
  ): Future[Option[(DocumentRecord, RuntimeAccessLevel)]] = db.query { sql =>
    loggedInUser match {
      case Some(user) => {
        val records = 
          sql.selectFrom(DOCUMENT
               .leftJoin(SHARING_POLICY)
               .on(DOCUMENT.ID.equal(SHARING_POLICY.DOCUMENT_ID))
               .and(SHARING_POLICY.SHARED_WITH.equal(user)))
             .where(DOCUMENT.ID.equal(id))
             .fetchArray
             
        val grouped = groupLeftJoinResult(records, classOf[DocumentRecord], classOf[SharingPolicyRecord])
        if (grouped.size > 1)
          throw new RuntimeException("Got " + grouped.size + " DocumentRecords with the same ID: " + grouped.keys.map(_.getId).mkString(", "))
                      
        grouped.headOption.map { case (document, sharingPolicies) =>
          (document, determineAccessLevel(document, sharingPolicies, loggedInUser)) }
      }
      
      case None =>
        // Anonymous request - just retrieve document
        Option(sql.selectFrom(DOCUMENT).where(DOCUMENT.ID.equal(id)).fetchOne()).map(document =>
          (document, determineAccessLevel(document, Seq.empty[SharingPolicyRecord], loggedInUser)))
    }
  }

  /** Batch-retrieves the document records with the given IDs **/
  def getDocumentRecordsById(docIds: Seq[String]) = db.query { sql => 
    sql.selectFrom(DOCUMENT).where(DOCUMENT.ID.in(docIds)).fetchArray().toSeq
  }

  /** Batch.retrieves document records along with runtime access level **/
  def getDocumentRecordsByIdWithAccessLevel(docIds: Seq[String], loggedInUser: Option[String] = None) = db.query { sql =>
    loggedInUser match {
      case Some(user) => 
        sql.selectFrom(DOCUMENT
             .leftOuterJoin(SHARING_POLICY)
             .on(SHARING_POLICY.DOCUMENT_ID.equal(DOCUMENT.ID))
             .and(SHARING_POLICY.SHARED_WITH.equal(user))
            )
            .where(DOCUMENT.ID.in(docIds))
            .fetchArray.toSeq
            .map { record => 
              val document = record.into(classOf[DocumentRecord])
              val policy = record.into(classOf[SharingPolicyRecord])
              (document, determineAccessLevel(document, Seq(policy), loggedInUser))
            }

      case None => 
        sql.selectFrom(DOCUMENT)
           .where(DOCUMENT.ID.in(docIds))
           .fetchArray.toSeq
           .map { document =>
             (document, determineAccessLevel(document, Seq.empty[SharingPolicyRecord], loggedInUser))
           }
    }
  }

  /** Batch-retrieves the documents with the given IDs, adding extra part count
    * and content type Info
    */
  def getDocumentsById(docIds: Seq[String]) = db.query { sql =>
    if (docIds.isEmpty) {
      Seq.empty[MyDocument]
    } else {
      val idSet = docIds
        .flatMap(sanitizeDocId) // prevent injection attacks
        .map(id => s"'${id}'") // SQL quoting
        .mkString(",") // join

      // Note that this query does NOT maintain the order from the idSet
      val query = 
        s"""
        SELECT 
          document.*,
          file_count,
          content_types
        FROM document
        JOIN (
          SELECT
            count(*) AS file_count,
            array_agg(DISTINCT content_type) AS content_types,
            document_id
          FROM document_filepart
          GROUP BY document_id
        ) AS parts ON parts.document_id = document.id
        WHERE document.id IN (${idSet});
        """
        
      // Restore result order
      val documents = sql.resultQuery(query).fetchArray.map(MyDocument.build).toSeq
      docIds.flatMap(id => documents.find(_.document.getId == id))
    }
  }

  /** Retrieves extended document metadata, along with runtime access permissions.
    * 
    * Extended document metadata includes the DocumentRecord, all FilepartDocumentRecords
    * and the full user record of the document owner.
    */
  def getExtendedMeta(
    id: String, loggedInUser: Option[String] = None
  ): Future[Option[(ExtendedDocumentMetadata, RuntimeAccessLevel)]] = db.query { sql =>
    val records = loggedInUser match {
      case Some(username) =>
        // Retrieve with sharing policies that may apply
        sql.selectFrom(DOCUMENT
             .join(DOCUMENT_FILEPART).on(DOCUMENT.ID.equal(DOCUMENT_FILEPART.DOCUMENT_ID))
             .join(USER).on(DOCUMENT.OWNER.equal(USER.USERNAME))
             .leftJoin(SHARING_POLICY)
               .on(DOCUMENT.ID.equal(SHARING_POLICY.DOCUMENT_ID))
               .and(SHARING_POLICY.SHARED_WITH.equal(username)))
          .where(DOCUMENT.ID.equal(id))
          .fetchArray()

      case None =>
        // Anyonymous request - just retrieve parts and owner
        sql.selectFrom(DOCUMENT
             .join(DOCUMENT_FILEPART).on(DOCUMENT.ID.equal(DOCUMENT_FILEPART.DOCUMENT_ID))
             .join(USER).on(DOCUMENT.OWNER.equal(USER.USERNAME)))
           .where(DOCUMENT.ID.equal(id))
           .fetchArray()

    }

    val grouped = groupLeftJoinResult(records, classOf[DocumentRecord], classOf[DocumentFilepartRecord])
    if (grouped.size > 1)
      throw new RuntimeException("Got " + grouped.size + " DocumentRecords with the same ID: " + grouped.keys.map(_.getId).mkString(", "))

    val sharingPolicies = records.map(_.into(classOf[SharingPolicyRecord])).filter(isNotNull(_)).distinct

    // Return with parts sorted by sequence number
    grouped.headOption.map { case (document, parts) =>
      val owner = records.head.into(classOf[UserRecord])      
      (ExtendedDocumentMetadata(document, parts.sortBy(_.getSequenceNo), owner), determineAccessLevel(document, sharingPolicies, loggedInUser))
    }
  }

  /** List all documents in the owner's root folder **/
  def listRootIdsByOwner(owner: String) = db.query { sql => 
    sql.select(DOCUMENT.ID)
      .from(DOCUMENT)
      .fullOuterJoin(FOLDER_ASSOCIATION)
        .on(FOLDER_ASSOCIATION.DOCUMENT_ID.equal(DOCUMENT.ID))
      .where(DOCUMENT.OWNER.equal(owner)
        .and(FOLDER_ASSOCIATION.FOLDER_ID.isNull))
      .fetch(0, classOf[String])
      .toSeq
  }

  /** List all document IDs owned by the given user. 
    *
    * This info is only required for account removal.
    */
  def listAllIdsByOwner(owner: String) = db.query { sql =>
    sql.select(DOCUMENT.ID)
       .from(DOCUMENT)
       .where(DOCUMENT.OWNER.equal(owner))
       .fetch(0, classOf[String])
       .toSeq
  }

  /** Used by private/public account info API method **/
  def countAllByOwner(owner: String, publicOnly: Boolean = false) = db.query { sql =>
    if (publicOnly)
      sql.selectCount().from(DOCUMENT)
         .where(DOCUMENT.OWNER.equal(owner)
         .and(DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString)))
         .fetchOne(0, classOf[Int])
    else
      sql.selectCount().from(DOCUMENT)
         .where(DOCUMENT.OWNER.equal(owner))
         .fetchOne(0, classOf[Int])
  }

  /** Reads the document preferences for the given document **/
  def getDocumentPreferences(docId: String) = db.query { sql =>
    sql.selectFrom(DOCUMENT_PREFERENCES)
       .where(DOCUMENT_PREFERENCES.DOCUMENT_ID.equal(docId))
       .fetchArray.toSeq
  }

}