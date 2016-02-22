package storage

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import java.io.File
import org.elasticsearch.common.settings.ImmutableSettings
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.io.Source

object ES {
  
  val client = {    
    // Initialize the client
    val settings = 
      ImmutableSettings.settingsBuilder()
        .put("http.enabled", false)
        .put("path.home", "data")

    val client = ElasticClient.local(settings.build)

    // Check if index 'recogito' exists, create (with mappoings) if not
    client.execute { indexExists("recogito") }.map(response =>
      if (!response.isExists) {
        val create = client.admin.indices().prepareCreate("recogito")
        loadMappings.foreach { case (name, json) => create.addMapping(name, json) }
        create.execute().actionGet()
      }).await
 
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
