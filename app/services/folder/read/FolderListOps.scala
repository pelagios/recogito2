package services.folder.read

import java.util.UUID
import org.jooq.Record
import scala.concurrent.Future
import scala.collection.JavaConversions._
import services.Page
import services.folder.FolderService
import services.generated.Tables.FOLDER
import services.generated.tables.records.{FolderRecord, SharingPolicyRecord}

trait FolderListOps { self: FolderService =>

  /** Helper to marshal a combined folder/sharingpolicy record **/
  private def asAccessibleFolder(record: Record) = {
    val folder = record.into(classOf[FolderRecord])
    val policy = {
      val p = record.into(classOf[SharingPolicyRecord])
      Option(p.getId).map(_ => p)
    }
    val subfolderCount =
      Option(record.getValue("subfolder_count", classOf[Integer])).map(_.toInt).getOrElse(0)

    (folder, policy, subfolderCount)
  }

  /** 'ls'-like command, lists folders by an owner, in the root or a subdirectory **/
  def listFolders(
    owner: String, 
    offset: Int, 
    size: Int, 
    parent: Option[UUID]
  ): Future[Page[(FolderRecord, Int)]] = 
    db.query { sql => 
      val startTime = System.currentTimeMillis

      // Helper
      def asTuple(record: Record) = {
        val folder = record.into(classOf[FolderRecord])
        val subfolderCount =
          Option(record.getValue("subfolder_count", classOf[Integer])).map(_.toInt).getOrElse(0)
        (folder, subfolderCount)
      }

      val total = parent match {
        case Some(parentId) =>
          sql.selectCount()
             .from(FOLDER)
             .where(FOLDER.OWNER.equal(owner))
             .and(FOLDER.PARENT.equal(parentId))
             .fetchOne(0, classOf[Int])

        case None => // Root folder
          sql.selectCount()
             .from(FOLDER)
             .where(FOLDER.OWNER.equal(owner))
             .and(FOLDER.PARENT.isNull)
             .fetchOne(0, classOf[Int])
      }

      val items = parent match {
        case Some(parentId) =>
          val query = 
            s"""
             SELECT
               folder.*,
               subfolder_count
             FROM folder
               LEFT OUTER JOIN (
                 SELECT parent, count(*) subfolder_count 
                 FROM folder
                 GROUP BY parent
               ) children ON children.parent = folder.id
             WHERE folder.owner = ? AND folder.parent = ?
             ORDER BY title ASC
             LIMIT $size
             OFFSET $offset           
            """
          sql.resultQuery(query, owner, parentId).fetchArray.map(asTuple)

        case None => // Root folder
          val query = 
            s"""
             SELECT
               folder.*,
               subfolder_count
             FROM folder
               LEFT OUTER JOIN (
                 SELECT parent, count(*) subfolder_count 
                 FROM folder
                 GROUP BY parent
               ) children ON children.parent = folder.id
             WHERE folder.owner = ? AND folder.parent IS NULL
             ORDER BY title ASC
             LIMIT $size
             OFFSET $offset           
            """
          sql.resultQuery(query, owner).fetchArray.map(asTuple)
      }

      Page(System.currentTimeMillis - startTime, total, offset, size, items)
    }  

  /** Lists folders Shared with Me, in the root or a subdirectory **/
  def listFoldersSharedWithMe(
    username: String, 
    parent: Option[UUID]
  ): Future[Page[(FolderRecord, SharingPolicyRecord, Int)]] =
    db.query { sql =>

      // TODO implement proper totals count, offset, sorting
      val startTime = System.currentTimeMillis

      // Helper
      def asTuple(record: Record) = {
        val folder = record.into(classOf[FolderRecord])
        val policy = record.into(classOf[SharingPolicyRecord])
        val subfolderCount =
          Option(record.getValue("subfolder_count", classOf[Integer])).map(_.toInt).getOrElse(0)
        (folder, policy, subfolderCount)
      }

      val query = parent match {
        case Some(parentId) => 
          val query = 
            """
            SELECT 
              folder.*,
              sharing_policy.*
              subfolder_count
            FROM sharing_policy
              JOIN folder ON folder.id = sharing_policy.folder_id
              LEFT OUTER JOIN (
                SELECT parent, count(*) subfolder_count
                FROM folder
                GROUP BY parent
              ) children ON children.parent = folder.id
            WHERE shared_with = ? AND folder.parent = ?;
            """
          sql.resultQuery(query, username, parentId)

        case None => 
          val query = 
            """
            SELECT 
              sharing_policy.*, 
              folder.*, 
              parent_sharing_policy.shared_with AS parent_shared,
              subfolder_count
            FROM sharing_policy
              JOIN folder ON folder.id = sharing_policy.folder_id
              LEFT OUTER JOIN folder parent_folder ON parent_folder.id = folder.parent
              LEFT OUTER JOIN sharing_policy parent_sharing_policy ON parent_sharing_policy.folder_id = parent_folder.id
              LEFT OUTER JOIN (
                SELECT parent, count(*) subfolder_count
                FROM folder
                GROUP BY parent
              ) children ON children.parent = folder.id
            WHERE 
              sharing_policy.shared_with = ? AND
              parent_sharing_policy IS NULL;
            """
          sql.resultQuery(query, username)
      }

      val records = query.fetchArray.map(asTuple)
      Page(System.currentTimeMillis - startTime, records.size, 0, records.size, records)
    }

    /** Lists folders accessible in the public profile of a user, at root leve **/
    private def listAccessibleFoldersAtRoot(
      username: String, 
      loggedInAs: Option[String]
    ): Future[Page[(FolderRecord, Option[SharingPolicyRecord], Int)]] = db.query { sql =>
      val startTime = System.currentTimeMillis

      val query = loggedInAs match {

        case Some(loggedIn) =>
          val query = 
            """
            SELECT 
              folder.*,
              subfolder_count
            FROM folder
              LEFT OUTER JOIN sharing_policy 
                ON sharing_policy.folder_id = folder.id 
                  AND sharing_policy.shared_with = ?
              LEFT OUTER JOIN (
                SELECT parent, count(*) subfolder_count
                FROM folder
                GROUP BY parent
              ) children ON children.parent = folder.id
            WHERE 
              folder.owner = ?
                AND
              folder.parent IS NULL
                AND (
                  folder.public_visibility = 'PUBLIC' OR
                    sharing_policy.shared_with = ?
                );
            """
          sql.resultQuery(query, loggedIn, username, loggedIn)

        case None => 
          val query = 
            """
            SELECT
              folder.*,
              subfolder_count
            FROM folder
              LEFT OUTER JOIN (
                SELECT parent, count(*) subfolder_count
                FROM folder
                GROUP BY parent
              ) children ON children.parent = folder.id
            WHERE 
              folder.owner = ?
                AND
              folder.parent IS NULL
                AND 
              folder.public_visibility = 'PUBLIC';
            """
          sql.resultQuery(query, username)
      }

      val records = query.fetchArray.map(asAccessibleFolder)
      Page(System.currentTimeMillis - startTime, records.size, 0, records.size, records)
    }

    /** Lists subfolders accessible in the public view, in the given folder **/
    private def listAccessibleSubFolders(
      loggedInAs: Option[String],
      folder: UUID
    ): Future[Page[(FolderRecord, Option[SharingPolicyRecord], Int)]] = db.query { sql => 
      val startTime = System.currentTimeMillis
      
      val query = loggedInAs match {

        case Some(loggedIn) =>
          val query = 
            """
            SELECT 
              folder.*,
              subfolder_count 
            FROM folder
              LEFT OUTER JOIN sharing_policy 
                ON sharing_policy.folder_id = folder.id 
                  AND sharing_policy.shared_with = ?
              LEFT OUTER JOIN (
                SELECT parent, count(*) subfolder_count
                FROM folder
                GROUP BY parent
              ) children ON children.parent = folder.id
            WHERE folder.parent = ?
              AND (
                folder.public_visibility = 'PUBLIC' OR
                  sharing_policy.shared_with = ?
              );
            """
          sql.resultQuery(query, loggedIn, folder, loggedIn)

        case None =>
          val query = 
            """
            SELECT 
              folder.*,
              subfolder_count 
            FROM folder
              LEFT OUTER JOIN (
                SELECT parent, count(*) subfolder_count
                FROM folder
                GROUP BY parent
              ) children ON children.parent = folder.id
            WHERE folder.parent = ?
              AND folder.public_visibility = 'PUBLIC';
            """
          sql.resultQuery(query, folder)
      }

      val records = query.fetchArray.map(asAccessibleFolder)
      Page(System.currentTimeMillis - startTime, records.size, 0, records.size, records)
    }

    /** Lists (sub)folders accessible in the public view. 
      * 
      * Delegates to the appropriate private method above.
      */
    def listAccessibleFolders(
      username: String,
      loggedInAs: Option[String],
      parent: Option[UUID]
    ): Future[Page[(FolderRecord, Option[SharingPolicyRecord], Int)]] = parent match {
      case Some(folderId) => listAccessibleSubFolders(loggedInAs, folderId)
      case None => listAccessibleFoldersAtRoot(username, loggedInAs)
    }

}