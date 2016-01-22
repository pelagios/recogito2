import java.sql.Connection
import org.jooq.impl.DSL
import org.jooq.{ SQLDialect, DSLContext }
import play.api.db.DB
import play.api.Play.current
import play.api.{ Application, GlobalSettings, Logger }
import scala.io.Source
import scala.concurrent.ExecutionContext

object Global extends GlobalSettings {
  
  private val SCHEMA_SQL = "conf/schema.sql"

  override def onStart(app: Application) {   
    DB.withConnection { connection =>
      if (DSL.using(connection, database.DB.CURRENT_SQLDIALECT).meta().getTables.isEmpty()) {
        Logger.info("Empty database - initializing...")
        initDB(connection)
      }
    }
  }
  
  private def initDB(connection: Connection) = {
    val sql = Source.fromFile(SCHEMA_SQL)("UTF-8").getLines().mkString("\n")
    val stmt = connection.createStatement
    stmt.execute(sql)
  }

}
