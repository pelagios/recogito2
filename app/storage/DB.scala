package storage

import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import java.sql.Connection
import javax.inject.{ Inject, Singleton }
import services.user.UserService
import services.user.Roles
import org.jooq.impl.DSL
import org.jooq.{ SQLDialect, DSLContext }
import play.api.db.Database
import scala.collection.JavaConversions._
import scala.concurrent.{ ExecutionContext, Future }
import scala.io.Source

object DB {
  
  val CURRENT_SQLDIALECTT = SQLDialect.POSTGRES_9_4

}

/** cf. http://blog.jooq.org/2016/01/14/reactive-database-access-part-3-using-jooq-with-scala-futures-and-actors/ **/
class DB @Inject() (db: Database, system: ActorSystem) {

  private val databaseContext: ExecutionContext = system.dispatchers.lookup("contexts.database")
  
  /** Connection helpers **/
  
  def query[A](block: DSLContext => A): Future[A] = Future {
    db.withConnection { connection =>
      val sql = DSL.using(connection, DB.CURRENT_SQLDIALECTT)
      block(sql)
    }
  }(databaseContext)
  
  def withTransaction[A](block: DSLContext => A): Future[A] = Future {
    db.withTransaction { connection =>
      val sql = DSL.using(connection, DB.CURRENT_SQLDIALECTT)
      block(sql)
    }
  }(databaseContext)

}

class DBModule extends AbstractModule {

  def configure = {
    bind(classOf[DBInitializer]).asEagerSingleton
  }
  
}

@Singleton
class DBInitializer @Inject() (db: Database, userService: UserService, implicit val ctx: ExecutionContext) {

  // Does the user table exist? Run schema generation if not.
  db.withConnection { connection =>
    if (!DSL.using(connection, DB.CURRENT_SQLDIALECTT).meta().getTables.map(_.getName.toLowerCase).contains("user")) {
      play.api.Logger.info("Empty database - initializing...")
      initDB(connection)
    }
  }
  
  /** Database setup **/
  private def initDB(connection: Connection) = {
    
    // Splitting by ; is not 100% robust - but should be sufficient for our own schema file
    val statement = connection.createStatement

    Source.fromFile("conf/schema.sql", "UTF-8")
      .getLines().map(_.trim)
      .filter(line => !(line.startsWith("--") || line.isEmpty))
      .mkString(" ").split(";")
      .foreach(s => {
        statement.addBatch(s + ";")
      })

    statement.executeBatch()
    statement.close()    
    
    val f = for {
      _ <- userService.insertUser("recogito", "recogito@example.com", "recogito")
      _ <- userService.insertUserRole("recogito", Roles.Admin)
    } yield()
    
    f.recover { case t: Throwable => t.printStackTrace() }
  } 
  
}
