package services.document.read

import collection.JavaConversions._
import play.api.Logger
import scala.concurrent.Future
import services.{PublicAccess, RuntimeAccessLevel, SharingLevel}
import services.document.{DocumentService, ExtendedDocumentMetadata}
import services.generated.Tables.{DOCUMENT, DOCUMENT_FILEPART, SHARING_POLICY, USER}
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

}