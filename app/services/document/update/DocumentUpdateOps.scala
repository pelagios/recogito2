package services.document.update

import java.util.UUID
import play.api.Logger
import scala.concurrent.Future
import services.PublicAccess
import services.document.{DocumentService, License, PartOrdering}
import services.generated.tables.records.DocumentPreferencesRecord
import services.generated.Tables.{DOCUMENT, DOCUMENT_FILEPART, DOCUMENT_PREFERENCES}

/** Various document update operations **/
trait DocumentUpdateOps { self: DocumentService => 

  /** Updates document metadata for the given document ID **/
  def updateMetadata(
    docId: String,
    title: String,
    author: Option[String],
    dateFreeform: Option[String], 
    description: Option[String],
    language: Option[String],
    source: Option[String],
    edition: Option[String],
    license: Option[String],
    attribution: Option[String]
  ): Future[Boolean] = db.withTransaction { sql =>  
      
    val q = sql.update(DOCUMENT)
      .set(DOCUMENT.TITLE, title)
      .set(DOCUMENT.AUTHOR, optString(author))
      .set(DOCUMENT.DATE_FREEFORM, optString(dateFreeform))
      .set(DOCUMENT.DESCRIPTION, optString(description))
      .set(DOCUMENT.LANGUAGE, optString(language))
      .set(DOCUMENT.SOURCE, optString(source))
      .set(DOCUMENT.EDITION, optString(edition))
      .set(DOCUMENT.LICENSE, optString(license))
      .set(DOCUMENT.ATTRIBUTION, optString(attribution))

    // If the update sets the document to a non-open license, make sure is_public is set to false
    val hasNonOpenLicense = license.map(acronym =>
      License.fromAcronym(acronym).map(!_.isOpen).getOrElse(true)).getOrElse(true)
      
    val rowsAffected =
      if (hasNonOpenLicense)
        q.set(DOCUMENT.PUBLIC_VISIBILITY, PublicAccess.PRIVATE.toString)
         .set(DOCUMENT.PUBLIC_ACCESS_LEVEL, null.asInstanceOf[String])
         .where(DOCUMENT.ID.equal(docId))
         .execute()
      else
        q.where(DOCUMENT.ID.equal(docId)).execute()
    
    rowsAffected == 1
  }

  /** Updates the public_access_level field with the given value (or null) **/
  def setPublicAccessLevel(docId: String, accessLevel: Option[PublicAccess.AccessLevel]) =
    db.query { sql =>
      sql.update(DOCUMENT)
         .set(DOCUMENT.PUBLIC_ACCESS_LEVEL, optString(accessLevel.map(_.toString)))
         .where(DOCUMENT.ID.equal(docId)).execute() == 1
    }

  /** Updates the public_visibility field with the given value **/
  def setPublicVisibility(docId: String, visibility: PublicAccess.Visibility) = 
    db.query { sql => 
      sql.update(DOCUMENT)
        .set(DOCUMENT.PUBLIC_VISIBILITY, visibility.toString)
        .where(DOCUMENT.ID.equal(docId)).execute() == 1
    }

  /** Updates the public visibility and access level fields in one go **/
  def setPublicAccessOptions(docId: String, visibility: PublicAccess.Visibility, accessLevel: Option[PublicAccess.AccessLevel] = None) =
    db.withTransaction { sql =>
      sql.update(DOCUMENT)
         .set(DOCUMENT.PUBLIC_VISIBILITY, visibility.toString)
         .set(DOCUMENT.PUBLIC_ACCESS_LEVEL, optString(accessLevel.map(_.toString)))
         .where(DOCUMENT.ID.equal(docId)).execute() == 1
    }

    
  /** Updates document filepart metadata for the given filepart ID **/
  def updateFilepartMetadata(docId: String, partId: UUID, title: String, source: Option[String]) = db.withTransaction { sql =>
    val rowsAffected = sql.update(DOCUMENT_FILEPART)
      .set(DOCUMENT_FILEPART.TITLE, title)
      .set(DOCUMENT_FILEPART.SOURCE, optString(source))
      .where(DOCUMENT_FILEPART.DOCUMENT_ID.equal(docId).and(DOCUMENT_FILEPART.ID.equal(partId)))
      .execute()
      
    rowsAffected == 1
  }

  /** Updates the sequence numbers of fileparts for a specific document **/
  def setFilepartSortOrder(docId: String, sortOrder: Seq[PartOrdering]) = db.withTransaction { sql =>
    // To verify validaty of the request, load the fileparts from the DB first...
    val fileparts = 
      sql.selectFrom(DOCUMENT_FILEPART).where(DOCUMENT_FILEPART.DOCUMENT_ID.equal(docId)).fetchArray()
    
    // ...discard parts that are not associated with the document and log a warning
    val foundIds = fileparts.map(_.getId).toSet
    val requestedIds = sortOrder.map(_.partId).toSet
    if (requestedIds != foundIds)
      Logger.warn("Attempt to re-order fileparts that don't belong to the specified doc")
    val sanitizedOrder = sortOrder.filter(ordering => foundIds.contains(ordering.partId))
    
    // Should normally be empty
    val unchangedParts = fileparts.filter(part => !requestedIds.contains(part.getId))
    if (unchangedParts.size > 0)
      Logger.warn("Request for re-ordering fileparts is missing " + unchangedParts.size + " rows")
   
    // There is no uniquness constraint in the DB on (documentId, seqNo), since we wouldn't be able to
    // update sequence numbers without changing part IDs then. Therefore we enforce uniqueness here.
    val updatedSequenceNumbers = sanitizedOrder.map(_.seqNo) ++ unchangedParts.map(_.getSequenceNo)
    if (updatedSequenceNumbers.size != updatedSequenceNumbers.distinct.size)
      throw new Exception("Uniqueness constraint violated for Filepart (document_id, sequence_no)")
      
    // Update fileparts in DB
    val updates = sanitizedOrder.map(ordering =>
      sql.update(DOCUMENT_FILEPART)
         .set(DOCUMENT_FILEPART.SEQUENCE_NO, ordering.seqNo.asInstanceOf[java.lang.Integer])
         .where(DOCUMENT_FILEPART.ID.equal(ordering.partId)))

    sql.batch(updates:_*).execute()
  }

  def upsertPreferences(docId: String, name: String, prefs: String): Future[Boolean] = db.query { sql =>
    val prefRecord = new DocumentPreferencesRecord(docId, name, prefs)
    sql.insertInto(DOCUMENT_PREFERENCES)
       .set(prefRecord)
       .onDuplicateKeyUpdate()
       .set(prefRecord)
       .execute() == 1
  }

  def deletePreferences(docId: String, name: String): Future[Boolean] = db.query { sql => 
    sql.deleteFrom(DOCUMENT_PREFERENCES)
       .where(DOCUMENT_PREFERENCES.DOCUMENT_ID.equal(docId)
         .and(DOCUMENT_PREFERENCES.PREFERENCE_NAME.equal(name)))
       .execute() == 1
  }
  
}