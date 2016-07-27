import play.api.{ Application, GlobalSettings }
import storage.ES

object Global extends GlobalSettings {
  
  private val SCHEMA_SQL = "conf/schema.sql"

  override def onStart(app: Application) {
    ES.start()
  }
  
  override def onStop(app: Application) {
    ES.stop()
  }

}
