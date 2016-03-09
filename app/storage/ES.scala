package storage

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import java.io.File
import org.elasticsearch.common.settings.ImmutableSettings
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.io.Source
import scala.concurrent.duration._
import scala.language.postfixOps

object ES {
  
  val IDX_RECOGITO = "recogito"

  lazy val client = {
    val settings =
      ImmutableSettings.settingsBuilder()
        .put("http.enabled", true)
        .put("path.home", "index")

    ElasticClient.local(settings.build)
  }
  
  def start() = {
    Logger.info("Starting ElasticSearch local node")
    
    implicit val timeout = 60 seconds
    val response = client.execute { index exists(IDX_RECOGITO) }.await
    
    if (!response.isExists()) {
      Logger.info("No ES index - initializing...")
      val create = client.admin.indices().prepareCreate(IDX_RECOGITO)
      loadMappings.foreach { case (name, json) =>  { 
        Logger.info("Create mapping - " + name)
        create.addMapping(name, json) 
      }}
      create.execute().actionGet()
    }
  }
  
  def stop() = {
    Logger.info("Stopping ElasticSearch local node")
    client.close()
  }

  /** Loads all JSON files from the mappings directory **/
  private def loadMappings(): Seq[(String, String)] =
    new File("conf/es-mappings").listFiles.toSeq.filter(_.getName.endsWith(".json")).map(f => {
      val name = f.getName.substring(0, f.getName.lastIndexOf('.'))
      val json = Source.fromFile(f).getLines.mkString("\n")
      (name, json)
    })
    
}
