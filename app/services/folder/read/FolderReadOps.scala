package services.folder.read

import java.util.UUID
import org.jooq.Record
import scala.concurrent.Future
import scala.collection.JavaConversions._
import services.folder.FolderService
import services.generated.Tables.{FOLDER, SHARING_POLICY}
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

  /** Returns the list of collaborators on this folder **/
  def getFolderCollaborators(id: UUID) = db.query { sql => 
    sql.selectFrom(SHARING_POLICY)
       .where(SHARING_POLICY.FOLDER_ID.equal(id))
       .fetchArray
       .toSeq
  }

}