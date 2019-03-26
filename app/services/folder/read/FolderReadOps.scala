package services.folder.read

import java.util.UUID
import org.jooq.Record
import scala.concurrent.Future
import scala.collection.JavaConversions._
import services.folder.FolderService
import services.generated.Tables.{FOLDER, FOLDER_ASSOCIATION, SHARING_POLICY}
import services.generated.tables.records.{FolderRecord, SharingPolicyRecord}

trait FolderReadOps { self: FolderService => 

  /** Marshals a joined folder/sharingpolicy record to a typed tuple **/
  private def toFolderAndPolicy(
    record: Record
  ): (FolderRecord, Option[SharingPolicyRecord]) = {
    val folder = record.into(classOf[FolderRecord])
    val policy = record.into(classOf[SharingPolicyRecord])

    // If there is no policy stored, the record will still be there, but 
    // with all fields == null
    if (policy.getSharedWith == null)
      (folder, None)
    else 
      (folder, Some(policy))
  }

  /** Returns the folder with the given ID, plus the sharing policy 
    * associated with the given user, if any.
    */
  def getFolder(
    id: UUID,
    loggedInAs: String
  ): Future[Option[(FolderRecord, Option[SharingPolicyRecord])]] = db.query { sql => 
    val record = 
      sql.select().from(FOLDER)
         .leftOuterJoin(SHARING_POLICY)
           .on(SHARING_POLICY.FOLDER_ID.equal(FOLDER.ID)
             .and(SHARING_POLICY.SHARED_WITH.equal(loggedInAs)))
         .where(FOLDER.ID.equal(id))
         .fetchOne

    Option(record).map(toFolderAndPolicy)
  }

  /** Returns the requested folders (if they exist), along with the sharing  
    * policies for the given user, if any
    */
  def getFolders(
    ids: Seq[UUID],
    loggedInAs: String
  ): Future[Seq[(FolderRecord, Option[SharingPolicyRecord])]] = db.query { sql => 
    val records = 
      sql.select().from(FOLDER)
         .leftOuterJoin(SHARING_POLICY)
           .on(SHARING_POLICY.FOLDER_ID.equal(FOLDER.ID)
             .and(SHARING_POLICY.SHARED_WITH.equal(loggedInAs)))
         .where(FOLDER.ID.in(ids))
         .fetchArray

    records.map(toFolderAndPolicy)
  }

  /** A flattended list of IDs of all children below the given folder **/
  def getAllSubfoldersRecursive(id: UUID) = db.query { sql => 
    val query =
      """
      WITH RECURSIVE path AS (
        SELECT 
          id, title, parent,
          ARRAY[id] AS path_ids,
          ARRAY[title] AS path_titles
        FROM folder
        UNION ALL
          SELECT
            f.id, f.title, f.parent,
            p.path_ids || f.id,
            p.path_titles || f.title
          FROM folder f
          JOIN path p on f.id = p.parent
      )
      SELECT
        path_ids[1] AS id,
        path_titles[1] AS title
      FROM path WHERE parent=?;
      """

    sql.resultQuery(query, id).fetchArray.map { record => 
      record.into(classOf[(UUID, String)])
    }.toSeq
  }

  /** Returns the folder the given document is in (if any) **/
  def getContainingFolder(documentId: String) = db.query { sql =>
    Option(
      sql.select().from(FOLDER_ASSOCIATION)
         .join(FOLDER)
           .on(FOLDER.ID.equal(FOLDER_ASSOCIATION.FOLDER_ID))
         .where(FOLDER_ASSOCIATION.DOCUMENT_ID.equal(documentId))
         .fetchOne
    ).map(_.into(classOf[FolderRecord]))
  }

  /** Returns the list of collaborators on this folder **/
  def getFolderCollaborators(id: UUID) = db.query { sql => 
    sql.selectFrom(SHARING_POLICY)
       .where(SHARING_POLICY.FOLDER_ID.equal(id))
       .fetchArray
       .toSeq
  }

}