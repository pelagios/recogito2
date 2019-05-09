package storage.es

import com.sksamuel.elastic4s.{ElasticsearchClientUri, TcpClient}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.bulk.RichBulkResponse
import java.io.File
import javax.inject.{Inject, Singleton}
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.XContentType
import play.api.{Configuration, Logger}
import play.api.inject.ApplicationLifecycle
import scala.io.Source
import scala.concurrent.{Await, Future, ExecutionContext}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}


/** Constants **/
object ES extends ESSearchSanitizer {

  // Index name
  val RECOGITO = "recogito"

  // Mapping type names
  val ANNOTATION         = "annotation"
  val ANNOTATION_HISTORY = "annotation_history"
  val ENTITY             = "entity"
  val CONTRIBUTION       = "contribution"
  val VISIT              = "visit"

  // Maximum response size in ES
  val MAX_SIZE           = 10000

  // Max. number of retries to do in case of failed imports
  val MAX_RETRIES        = 10
  
  // Max. number of boolean clauses in ES
  val MAX_CLAUSES        = 1024

  def logFailures(response: RichBulkResponse): Boolean = {
    if (response.hasFailures)
      response.failures.map(f => Logger.warn(f.failureMessage))

    !response.hasFailures
  }

}

/** ElasticSearch client + start & stop helpers **/
@Singleton
class ES @Inject() (config: Configuration, lifecycle: ApplicationLifecycle) {

  start()

  lifecycle.addStopHook { () => Future.successful(stop()) }

  lazy val client = {
    val host = config.getOptional[String]("es.host").getOrElse("localhost")
    val port = config.getOptional[Int]("es.port").getOrElse(9300)
    val clusterName = config.getOptional[String]("es.cluster.name").getOrElse("elasticsearch")
    val remoteClient = TcpClient.transport(Settings.builder().put("cluster.name", clusterName).build(),ElasticsearchClientUri(host, port))

    Try(Await.result(remoteClient execute { clusterStats }, 3.seconds)) match {
      case Success(_) =>
        Logger.info("Joining ElasticSearch cluster")
        remoteClient

      case Failure(_) =>
        throw new RuntimeException("Local fallback no longer supported. Please ensure you have a working ElasticSearch installation.")
    }
  }

  private def start() = {
    implicit val timeout = 60.seconds

    val response = client.execute { index exists(ES.RECOGITO) }.await

    if (!response.isExists()) {
      // No index - create index with all mappings
      Logger.info("No ES index - initializing...")

      val create = client.java.admin.indices().prepareCreate(ES.RECOGITO)
      create.setSettings(loadSettings(), XContentType.JSON)

      loadMappings().foreach { case (name, json) =>  {
        Logger.info("Create mapping - " + name)
        create.addMapping(name, json, XContentType.JSON)
      }}

      create.execute().actionGet()
    }
  }

  def flush = client execute flushIndex(ES.RECOGITO)

  private def stop() = {
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
      
  
  def countTotalDocs()(implicit ctx: ExecutionContext): Future[Long] =
    client execute {
      search(ES.RECOGITO) limit 0
    } map { _.totalHits }

}
