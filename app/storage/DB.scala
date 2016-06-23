package storage

import akka.actor.ActorSystem
import javax.inject.Inject
import org.jooq.impl.DSL
import org.jooq.{ SQLDialect, DSLContext }
import play.api.db.Database
import scala.concurrent.{ ExecutionContext, Future }

/** cf. http://blog.jooq.org/2016/01/14/reactive-database-access-part-3-using-jooq-with-scala-futures-and-actors/ **/
class DB @Inject() (db: Database, system: ActorSystem) {

  val databaseContext: ExecutionContext = system.dispatchers.lookup("contexts.database")

  def query[A](block: DSLContext => A): Future[A] = Future {
    db.withConnection { connection =>
      val sql = DSL.using(connection, DB.CURRENT_SQLDIALECT)
      block(sql)
    }
  }(databaseContext)
  
  def withTransaction[A](block: DSLContext => A): Future[A] = Future {
    db.withTransaction { connection =>
      val sql = DSL.using(connection, DB.CURRENT_SQLDIALECT)
      block(sql)
    }
  }(databaseContext)

}

object DB {
  
  val CURRENT_SQLDIALECT = SQLDialect.POSTGRES_9_4

}
