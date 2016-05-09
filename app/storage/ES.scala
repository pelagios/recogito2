package storage

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import java.io.File
import org.elasticsearch.common.settings.Settings
import play.api.{ Logger, Play }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.io.Source
import scala.concurrent.duration._
import scala.language.postfixOps

object ES {
  
  val IDX_RECOGITO = "recogito"

  lazy val client = {
    val home = Play.current.configuration.getString("recogito.index.dir") match {
      case Some(dir) => new File(dir)
      case None => new File("index")
    }
    
    val settings =
      Settings.settingsBuilder()
        .put("http.enabled", true)
        .put("path.home", home.getAbsolutePath)
        
    Logger.info("Using " + home.getAbsolutePath + " as index location")
    ElasticClient.local(settings.build)
  }
  
  def start() = {
    Logger.info("Starting ElasticSearch local node")
    
    implicit val timeout = 60 seconds
    val response = client.execute { index exists(IDX_RECOGITO) }.await
    
    if (!response.isExists()) {
      Logger.info("No ES index - initializing...")
      
      val create = client.admin.indices().prepareCreate(IDX_RECOGITO) 
      create.setSettings(loadSettings())
      
      loadMappings.foreach { case (name, json) =>  { 
        Logger.info("Create mapping - " + name)
        create.addMapping(name, json)
      }}
      
      create.execute().actionGet()
    }
  }
  
  def flushIndex =
    client execute {
      flush index ES.IDX_RECOGITO
    }
  
  def stop() = {
    Logger.info("Stopping ElasticSearch local node")
    client.close()
  }
  
  private def loadSettings(): String =
    Source.fromFile("conf/elasticsearch.json").getLines().mkString("\n")

  /** Loads all JSON files from the mappings directory **/
  private def loadMappings(): Seq[(String, String)] =
    new File("conf/es-mappings").listFiles.toSeq.filter(_.getName.endsWith(".json")).map(f => {
      val number = f.getName.substring(0, 2).toInt
      val name = f.getName.substring(3, f.getName.lastIndexOf('.'))
      val json = Source.fromFile(f).getLines.mkString("\n")
      (number, (name, json))
    }).sortBy(_._1).map(_._2)
    
}
