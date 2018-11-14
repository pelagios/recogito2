package services.folder

import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.collection.JavaConversions._
import scala.concurrent.Future
import services.{Page, BaseService}
import services.generated.Tables.FOLDER
import services.generated.tables.records.FolderRecord
import storage.db.DB

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

  def listFolders(owner: String, offset: Int, size: Int): Future[Page[FolderRecord]] = 
    db.query { sql => 
      val startTime = System.currentTimeMillis

      val total = sql.selectCount().from(FOLDER).fetchOne(0, classOf[Int])

      val items = 
        sql.selectFrom(FOLDER)
           .where(FOLDER.OWNER.equal(owner))
           .orderBy(FOLDER.TITLE.asc)
           .limit(size)
           .offset(offset)
           .fetch()
           .into(classOf[FolderRecord])

      Page(System.currentTimeMillis - startTime, total, offset, size, items)
    }  

  def createFolder(owner: String, title: String, parent: Option[UUID]): Future[FolderRecord] = 
    db.withTransaction { sql => 
      val folder = new FolderRecord(null, owner, title, optUUID(parent))
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

  def updateFolderTitle(id: UUID, title: String): Future[Boolean] = 
    db.withTransaction { sql => 
      sql.update(FOLDER)
        .set(FOLDER.TITLE, title)
        .where(FOLDER.ID.equal(id))
        .execute() == 1
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