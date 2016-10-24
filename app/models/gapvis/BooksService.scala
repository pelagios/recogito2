package models.gapvis

import javax.inject.{ Singleton, Inject }
import scala.concurrent.{ Future, ExecutionContext }
import play.api.Configuration
import play.api.cache.CacheApi
import models.generated.Tables._
import storage.{ DB, Uploads }
import models.{ BaseService }
import play.api.libs.json.Json
import models.generated.tables.records.{ DocumentRecord }
import scala.collection.JavaConversions._



@Singleton
class BooksService @Inject() (
    val config: Configuration,
    val uploads: Uploads,
    implicit val cache: CacheApi,
    implicit val ctx: ExecutionContext,
    implicit val db: DB
  ) extends BaseService {
  
  def list ()(implicit context: ExecutionContext): Future[List[Book]] = db.query { sql =>
    val docs = sql.selectFrom(DOCUMENT).fetch().into(classOf[DocumentRecord])
    var books = List[Book](); 
     docs.map( doc => {
       val book = Book(doc.getId,
           doc.getTitle, "", 
           doc.getAuthor, 
           doc.getDateFreeform)
       books = book :: books
     })
    books
  }
  
  def book(id: String)(implicit context: ExecutionContext): Future[Book] = db.query { sql =>
    val docs = sql.selectFrom(DOCUMENT).where("id='"+id+"'").fetch().into(classOf[DocumentRecord])
    val doc = docs(0)
    val book = Book(doc.getId,
       doc.getTitle, "", 
       doc.getAuthor, 
       doc.getDateFreeform)
    book
  }
}