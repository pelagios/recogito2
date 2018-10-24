package services.document

import collection.JavaConversions._
import java.sql.Timestamp
import java.util.Date
import org.jooq.Record21
import play.api.Logger
import services.{Page, SortOrder}
import services.generated.Tables._
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord, SharingPolicyRecord}
import storage.db.DB

trait SharingPolicies { self: DocumentService =>
  
  /** Upserts a document collaborator sharing policy (policies are unique by (document_id, shared_with) **/
  def addDocumentCollaborator(documentId: String, sharedBy: String, sharedWith: String, level: SharingLevel) = db.query { sql =>
    val (sharingPolicy, isNewCollaborator) = 
      Option(sql.selectFrom(SHARING_POLICY)
                .where(SHARING_POLICY.DOCUMENT_ID.equal(documentId)
                  .and(SHARING_POLICY.SHARED_WITH.equal(sharedWith)))
                .fetchOne()) match {
      
      case Some(policy) => {
        // There's a policy for this document/user pair already - update
        policy.setSharedBy(sharedBy)
        policy.setSharedAt(new Timestamp(new Date().getTime))
        policy.setAccessLevel(level.toString)
        (policy, false)
      }
        
      case None => {
        // Create new sharing policy
        val policy = new SharingPolicyRecord(null, null,
          documentId, 
          sharedBy,
          sharedWith,
          new Timestamp(new Date().getTime),
          level.toString)
   
        policy.changed(SHARING_POLICY.ID, false)     
        sql.attach(policy)
        (policy, true)
      }
      
    }
    
    sharingPolicy.store()
    (sharingPolicy, isNewCollaborator)
  } 
  
  /** Removes a document collaborator sharing policy **/
  def removeDocumentCollaborator(documentId: String, sharedWith: String) = db.query { sql =>
    sql.deleteFrom(SHARING_POLICY)
       .where(SHARING_POLICY.DOCUMENT_ID.equal(documentId)
         .and(SHARING_POLICY.SHARED_WITH.equal(sharedWith)))
       .execute() == 1
  } 
  
  /** Lists all collaborators on a specific document **/
  def listDocumentCollaborators(documentId: String) = db.query { sql =>
    sql.selectFrom(SHARING_POLICY).where(SHARING_POLICY.DOCUMENT_ID.equal(documentId)).fetchArray().toSeq
  }
  
  /** Returns the number of documents shared with a given user **/
  def countBySharedWith(sharedWith: String) = db.query { sql =>
    sql.selectCount().from(SHARING_POLICY).where(SHARING_POLICY.SHARED_WITH.equal(sharedWith)).fetchOne(0, classOf[Int])
  }
  
  /** Convenience method to list all document IDs shared with the given user **/
  def listAllIdsSharedWith(username: String) = db.query { sql => 
    sql.select(SHARING_POLICY.DOCUMENT_ID).from(SHARING_POLICY).where(SHARING_POLICY.SHARED_WITH.equal(username)).fetch(0, classOf[String]).toSeq
  }
  
  /** Lists the documents shared with a given user (paged response **/
  def findShared(
    sharedWith: String,
    offset: Int,
    limit: Int, 
    sortBy: Option[String] = None,
    sortOrder: Option[SortOrder] = None
  ) = db.query { sql =>
    val startTime = System.currentTimeMillis
    val sortField = sortBy.flatMap(fieldname => getSortField(Seq(DOCUMENT, SHARING_POLICY), fieldname, sortOrder))
    val total = sql.selectCount().from(SHARING_POLICY).where(SHARING_POLICY.SHARED_WITH.equal(sharedWith)).fetchOne(0, classOf[Int])
    
    val query = sql.selectFrom(SHARING_POLICY
          .join(DOCUMENT)
          .on(SHARING_POLICY.DOCUMENT_ID.equal(DOCUMENT.ID)))
        .where(SHARING_POLICY.SHARED_WITH.equal(sharedWith))
         
    val items = sortField match {
      case Some(sort) => query.orderBy(sort).limit(limit).offset(offset).fetchArray().toSeq
      case None => query.limit(limit).offset(offset).fetchArray().toSeq
    }
    
    val mapped = items
      .map(r => (r.into(classOf[DocumentRecord]), r.into(classOf[SharingPolicyRecord])))

    Page(System.currentTimeMillis - startTime, total, offset, limit, mapped)
  }

  def findSharedWithPart(
    sharedWith: String,
    offset: Int, 
    limit: Int, 
    sortBy: Option[String] = None, 
    sortOrder: Option[SortOrder] = None
  ) = db.query { sql => 
    val startTime = System.currentTimeMillis
    val sortField = sortBy.flatMap(fieldname => getSortField(Seq(DOCUMENT, SHARING_POLICY), fieldname, sortOrder))
    val total = sql.selectCount().from(SHARING_POLICY).where(SHARING_POLICY.SHARED_WITH.equal(sharedWith)).fetchOne(0, classOf[Int])

    // We'll retrieve all fields except SHARING_POLICY.ID, since this
    // would cause ambiguity issues in the query further down the line
    val subqueryFields = { 
      DOCUMENT.fields() ++
      SHARING_POLICY.fields() 
    } filter { _ != SHARING_POLICY.ID }

    val subquery = sortField match {
      case Some(f) => 
        sql.select(subqueryFields:_*)
           .from(SHARING_POLICY
             .join(DOCUMENT)
             .on(SHARING_POLICY.DOCUMENT_ID.equal(DOCUMENT.ID)))
           .where(SHARING_POLICY.SHARED_WITH.equal(sharedWith))
           .orderBy(f)
           .limit(limit)
           .offset(offset)

      case None =>
        sql.select(subqueryFields:_*)
           .from(SHARING_POLICY
             .join(DOCUMENT)
             .on(SHARING_POLICY.DOCUMENT_ID.equal(DOCUMENT.ID)))
           .where(SHARING_POLICY.SHARED_WITH.equal(sharedWith))
           .limit(20)
           .offset(0)
    }

    val rows = sql.select()
                  .from(subquery)
                  .join(DOCUMENT_FILEPART)
                  .on(subquery.field(SHARING_POLICY.DOCUMENT_ID).equal(DOCUMENT_FILEPART.DOCUMENT_ID))
                  .fetchArray

    val documentsAndPolicies = rows.map { r =>
      val document = r.into(classOf[DocumentRecord])
      val sharingPolicy = r.into(classOf[SharingPolicyRecord])
      (document, sharingPolicy)
    }.distinct

    val fileparts = rows.map(_.into(classOf[DocumentFilepartRecord])).toSeq

    val results = documentsAndPolicies.map { t =>
      val parts = fileparts.filter(_.getDocumentId == t._1.getId)
      (t._1, t._2, parts)
    }.toSeq
    
    Page(System.currentTimeMillis - startTime, total, offset, limit, results)
  }
  
  /** Deletes all policies shared by and with the given user **/
  def deleteAffectedPolicies(username: String) = db.withTransaction { sql =>
    sql.deleteFrom(SHARING_POLICY)
      .where(SHARING_POLICY.SHARED_WITH.equal(username)
        .or(SHARING_POLICY.SHARED_BY.equal(username)))
      .execute()
  }
  
  protected def determineAccessLevel(document: DocumentRecord, sharingPolicies: Seq[SharingPolicyRecord], forUser: Option[String]): RuntimeAccessLevel = {
    
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
  
}