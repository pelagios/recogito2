package services.folder

import java.util.UUID
import javax.inject.{Inject, Singleton}
import org.jooq.Record
import org.jooq.impl.DSL
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.collection.JavaConversions._
import scala.concurrent.Future
import services.{Page, BaseService, PublicAccess}
import services.generated.Tables.{DOCUMENT, FOLDER, FOLDER_ASSOCIATION, SHARING_POLICY}
import services.generated.tables.records.{FolderRecord, SharingPolicyRecord}
import storage.db.DB

@Singleton
class FolderService @Inject() (implicit val db: DB) extends BaseService
  with read.FolderReadOps
  with read.BreadcrumbReadOps
  with FolderAssociationService
  with SharedFolderService {

  /** A flattended list of IDs of all children below the given folder **/
  def getChildrenRecursive(id: UUID) = db.query { sql => 
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

  def deleteFolder(id: UUID): Future[Boolean] = 
    db.withTransaction { sql => 
      sql.deleteFrom(FOLDER).where(FOLDER.ID.equal(id)).execute == 1
    }

  def deleteByOwner(owner: String): Future[Boolean] = 
    db.withTransaction { sql => 
      sql.deleteFrom(FOLDER).where(FOLDER.OWNER.equal(owner)).execute == 1
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

  def updatePublicVisibility(id: UUID, value: PublicAccess.Visibility) = db.withTransaction { sql =>
    sql.update(FOLDER)
       .set(FOLDER.PUBLIC_VISIBILITY, value.toString)
       .where(FOLDER.ID.equal(id))
       .execute > 0
  }

  def updatePublicAccessLevel(id: UUID, value: PublicAccess.AccessLevel) = db.withTransaction { sql => 
    sql.update(FOLDER)
       .set(FOLDER.PUBLIC_ACCESS_LEVEL, value.toString)
       .where(FOLDER.ID.equal(id))
       .execute > 0
  }

  def updateVisibilitySettings(
    id: UUID,
    visibility: PublicAccess.Visibility, 
    accessLevel: Option[PublicAccess.AccessLevel]
  ) = db.withTransaction { sql => 
    sql.update(FOLDER)
       .set(FOLDER.PUBLIC_VISIBILITY, visibility.toString)
       .set(FOLDER.PUBLIC_ACCESS_LEVEL, optString(accessLevel.map(_.toString)))
       .where(FOLDER.ID.equal(id))
       .execute == 1
  }

}

object FolderService {

  implicit val folderWrites: Writes[FolderRecord] = (
    (JsPath \ "id").write[UUID] and
    (JsPath \ "owner").write[String] and
    (JsPath \ "title").write[String] and
    (JsPath \ "parent").writeNullable[UUID]
  )(f => (
    f.getId,
    f.getOwner,
    f.getTitle,
    Option(f.getParent)
  ))

}