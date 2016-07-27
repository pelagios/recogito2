package storage

import com.google.inject.AbstractModule
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import java.io.File
import org.elasticsearch.common.settings.ImmutableSettings
import play.api.{ Logger, Play }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.io.Source
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{ Try, Success, Failure }
import javax.inject.{ Inject, Singleton }
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future

object ES {
  
  val IDX_RECOGITO = "recogito"
  
  val MAX_SIZE = 2147483647

  lazy val client = {
    val home = Play.current.configuration.getString("recogito.index.dir") match {
      case Some(dir) => new File(dir)
      case None => new File("index")
    }
    
    val remoteClient = ElasticClient.remote("localhost", 9300)
    
    // Just fetch cluster stats to see if there's a cluster at all
    Try(
      remoteClient execute {
        get cluster stats
      }
    ) match {
      case Success(_) => {
        Logger.info("Joining ElasticSearch cluster")
        remoteClient 
      }
        
      case Failure(_) => {
        // No ES cluster available - instantiate a local client
        val settings =
          ImmutableSettings.settingsBuilder()
            .put("http.enabled", true)
            .put("path.home", home.getAbsolutePath)
        
        Logger.info("Local index - using " + home.getAbsolutePath + " as location")
        val client = ElasticClient.local(settings.build)
        
        // Introduce wait time, otherwise local index init is so slow that subsequent
        // .isExists request returns false despite an existing index (note: this is only
        // relevant in dev mode, anyway)
        Thread.sleep(1000)
        client
      }
    }
  }
  
  def start() = {
    implicit val timeout = 60 seconds
    val response = client.execute { index exists(IDX_RECOGITO) }.await
    
    if (response.isExists()) {
      // Index exists - create missing mappings as needed
      val list = client.admin.indices().prepareGetMappings()
      val existingMappings = list.execute().actionGet().getMappings().get(IDX_RECOGITO).keys.toArray.map(_.toString)
      loadMappings(existingMappings).foreach { case (name, json) =>
        Logger.info("Recreating mapping " + name)
        val putMapping = client.admin.indices().preparePutMapping(IDX_RECOGITO)
        putMapping.setType(name)
        putMapping.setSource(json)
        putMapping.execute().actionGet()
      }
    } else {
      // No index - create index with all mappings
      Logger.info("No ES index - initializing...")
      
      val create = client.admin.indices().prepareCreate(IDX_RECOGITO) 
      create.setSettings(loadSettings())
      
      loadMappings().foreach { case (name, json) =>  { 
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
  private def loadMappings(existingMappings: Seq[String] = Seq.empty[String]): Seq[(String, String)] =
    new File("conf/es-mappings").listFiles.toSeq.filter(_.getName.endsWith(".json"))
      .foldLeft(Seq.empty[(Int, (String, String))])((mappings, file)  => {
        val number = file.getName.substring(0, 2).toInt
        val name = file.getName.substring(3, file.getName.lastIndexOf('.'))
        if (existingMappings.contains(name)) {
          mappings
        } else {
          val json = Source.fromFile(file).getLines.mkString("\n")
          mappings :+ (number, (name, json))
        }
      }).sortBy(_._1).map(_._2)
    
}
