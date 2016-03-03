import java.sql.Connection
import org.jooq.impl.DSL
import org.jooq.{ SQLDialect, DSLContext }
import play.api.db.DB
import play.api.Play.current
import play.api.{ Application, GlobalSettings, Logger }
import scala.io.Source
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext
import models.UserService
import storage.ES

object Global extends GlobalSettings {
  
  private val SCHEMA_SQL = "conf/schema.sql"

  override def onStart(app: Application) {
    // Crude DB initialization check - does the user table exist? Run schema generation if not.
    DB.withConnection { connection =>
      if (!DSL.using(connection, storage.DB.CURRENT_SQLDIALECT).meta().getTables.map(_.getName.toLowerCase).contains("user")) {
        Logger.info("Empty database - initializing...")
        initDB(connection)
      }
    }
  }

  private def initDB(connection: Connection) = {
    // Splitting by ; is not 100% robust - but should be sufficient for our own schema file
    val statement = connection.createStatement

    Source.fromFile(SCHEMA_SQL)("UTF-8")
      .getLines().map(_.trim)
      .filter(line => !(line.startsWith("--") || line.isEmpty))
      .mkString(" ").split(";")
      .foreach(s => {
        statement.addBatch(s + ";")
      })

    statement.executeBatch()
    statement.close()    
  }

}
