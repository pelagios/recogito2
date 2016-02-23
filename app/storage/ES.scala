package storage

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import java.io.File
import org.elasticsearch.common.settings.ImmutableSettings
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.io.Source

object ES {

  val client = {
    // Initialize the client
    val settings =
      ImmutableSettings.settingsBuilder()
        .put("http.enabled", false)
        .put("path.home", "index")

    val client = ElasticClient.local(settings.build)

    // Check if index 'recogito' exists, create (with mappoings) if not
    val response = client.execute { indexExists("recogito") }.await
    if (!response.isExists) {
      Logger.info("No ES index - initializing...")
      val create = client.admin.indices().prepareCreate("recogito")
      loadMappings.foreach { case (name, json) =>  { 
        Logger.info("Create mapping - " + name)
        create.addMapping(name, json) 
      }}
      create.execute().actionGet()
    }

    client
  }

  /** Loads all JSON files from the mappings directory **/
  private def loadMappings(): Seq[(String, String)] =
    new File("conf/es-mappings").listFiles.toSeq.filter(_.getName.endsWith(".json")).map(f => {
      val name = f.getName.substring(0, f.getName.lastIndexOf('.'))
      val json = Source.fromFile(f).getLines.mkString("\n")
      (name, json)
    })
    
}
