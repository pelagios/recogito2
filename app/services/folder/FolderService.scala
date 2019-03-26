package services.folder

import java.util.UUID
import javax.inject.{Inject, Singleton}
import org.jooq.Record
import org.jooq.impl.DSL
import play.api.Logger
import scala.collection.JavaConversions._
import scala.concurrent.Future
import services.{Page, BaseService, PublicAccess}
import services.generated.Tables.{DOCUMENT, FOLDER, FOLDER_ASSOCIATION, SHARING_POLICY}
import services.generated.tables.records.{FolderRecord, SharingPolicyRecord}
import storage.db.DB

@Singleton
class FolderService @Inject() (implicit val db: DB) extends BaseService
  with create.CreateOps
  with read.FolderReadOps
  with read.BreadcrumbReadOps
  with update.UpdateOps
  with delete.DeleteOps {

  /** 'ls'-like command, lists folders by an owner, in the root or a subdirectory **/
  def listFolders(owner: String, offset: Int, size: Int, parent: Option[UUID]): Future[Page[FolderRecord]] = 
    db.query { sql => 
      val startTime = System.currentTimeMillis

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
          sql.selectFrom(FOLDER)
            .where(FOLDER.OWNER.equal(owner))
            .and(FOLDER.PARENT.equal(parentId))
            .orderBy(FOLDER.TITLE.asc)
            .limit(size)
            .offset(offset)
            .fetch()
            .into(classOf[FolderRecord])

        case None => // Root folder
          sql.selectFrom(FOLDER)
            .where(FOLDER.OWNER.equal(owner))
            .and(FOLDER.PARENT.isNull)
            .orderBy(FOLDER.TITLE.asc)
            .limit(size)
            .offset(offset)
            .fetch()
            .into(classOf[FolderRecord])
      }

      Page(System.currentTimeMillis - startTime, total, offset, size, items)
    }  

  def listFoldersSharedWithMe(username: String, parent: Option[UUID]): Future[Page[(FolderRecord, SharingPolicyRecord)]] =
    db.query { sql =>

      // TODO implement proper totals count, offset, sorting
      val startTime = System.currentTimeMillis

      // Helper
      def asTuple(record: Record) = {
        val folder = record.into(classOf[FolderRecord])
        val policy = record.into(classOf[SharingPolicyRecord])
        (folder, policy)
      }

      val query = parent match {
        case Some(parentId) => 
          // Subfolder
          val query = 
            """
            SELECT * 
            FROM sharing_policy
              JOIN folder ON folder.id = sharing_policy.folder_id
            WHERE shared_with = ? AND parent = ?;
            """
          sql.resultQuery(query, username, parentId)

        case None => 
          // Root folder
          val query = 
            """
            SELECT 
              sharing_policy.*, 
              folder.*, 
              parent_sharing_policy.shared_with AS parent_shared
            FROM sharing_policy
              JOIN folder ON folder.id = sharing_policy.folder_id
              LEFT OUTER JOIN folder parent_folder ON parent_folder.id = folder.parent
              LEFT OUTER JOIN sharing_policy parent_sharing_policy ON parent_sharing_policy.folder_id = parent_folder.id
            WHERE 
              sharing_policy.shared_with = ? AND
              parent_sharing_policy IS NULL;
            """
          sql.resultQuery(query, username)
      }

      val records = query.fetchArray.map(asTuple)
      Page(System.currentTimeMillis - startTime, records.size, 0, records.size, records)
    }

  def createFolder(owner: String, title: String, parent: Option[UUID]): Future[FolderRecord] = 
    db.withTransaction { sql => 
      val folder = new FolderRecord(UUID.randomUUID, owner, title, optUUID(parent), null, PublicAccess.PRIVATE.toString, null)
      sql.insertInto(FOLDER).set(folder).execute()
      folder
    }

  def renameFolder(id: UUID, title: String): Future[Boolean] = 
    db.withTransaction { sql => 
      sql.update(FOLDER)
        .set(FOLDER.TITLE, title)
        .where(FOLDER.ID.equal(id))
        .execute() == 1
    }

  def setReadme(id: UUID, readme: String): Future[Boolean] =
    db.withTransaction { sql =>
      sql.update(FOLDER)
         .set(FOLDER.README, readme)
         .where(FOLDER.ID.equal(id))
         .execute == 1
    }

  def deleteReadme(id: UUID): Future[Boolean] = setReadme(id, null)

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

}