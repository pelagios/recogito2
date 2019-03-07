package services.folder

import java.util.UUID
import javax.inject.{Inject, Singleton}
import org.jooq.impl.DSL
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.collection.JavaConversions._
import scala.concurrent.Future
import services.{Page, BaseService}
import services.generated.Tables.FOLDER
import services.generated.tables.records.FolderRecord
import storage.db.DB

case class Breadcrumb(id: UUID, title: String)

@Singleton
class FolderService @Inject() (implicit val db: DB) 
  extends BaseService with FolderAssociationService {

  def getFolder(id: UUID): Future[Option[FolderRecord]] = 
    db.query { sql => 
      Option(sql.selectFrom(FOLDER).where(FOLDER.ID.equal(id)).fetchOne)
    }

  def getFolders(ids: Seq[UUID]): Future[Seq[FolderRecord]] = 
    db.query { sql => 
      sql.selectFrom(FOLDER).where(FOLDER.ID.in(ids)).fetchArray
    }

  /** Gets the breadcrumb trail for a specific folder.
    * 
    * The breadcrumb trail includes the current folder (with the specified
    * id) and all parent folders in the hierarchy, top to bottom. I.e. the 
    * first item in the list is the top-most ancestor.
    */
  def getBreadcrumbs(id: UUID) = db.query { sql => 
    // Capitulation. Gave up re-modelling this query in JOOQ, sorry.
    val query = 
      """
      WITH RECURSIVE path AS (
        SELECT id, owner, title, parent, 
          ARRAY[id] AS path_ids,
          ARRAY[title] AS path_titles
        FROM folder
        WHERE parent is null
        UNION ALL
          SELECT f.id, f.owner, f.title, f.parent, 
            p.path_ids || f.id,
            p.path_titles || f.title
          FROM folder f
          JOIN path p on f.parent = p.id
      )
      SELECT path_ids, path_titles FROM path WHERE id=?;
      """

    Option(sql.resultQuery(query, id).fetchOne).map { result => 
      val ids = result.getValue("path_ids", classOf[Array[UUID]]).toSeq
      val titles = result.getValue("path_titles", classOf[Array[String]]).toSeq
      ids.zip(titles).map(t => Breadcrumb(t._1, t._2))
    } getOrElse {
      Seq.empty[Breadcrumb]
    }
  }


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
    }
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

  def createFolder(owner: String, title: String, parent: Option[UUID]): Future[FolderRecord] = 
    db.withTransaction { sql => 
      val folder = new FolderRecord(UUID.randomUUID, owner, title, optUUID(parent), null)
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