package database

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.common.settings.ImmutableSettings
import com.sksamuel.elastic4s.source.JsonDocumentSource
import play.api.Logger
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class AnnotationStorageSpec extends Specification {
  
  private val annotationMapping =
    Source.fromFile("conf/es-mappings/annotation.json")
          .getLines().filter(!_.startsWith("#")).mkString("\n")

  private def getClient() = {
    val settings = ImmutableSettings.settingsBuilder()
          .put("http.enabled", false)

    val client = ElasticClient.local(settings.build)
    val response = client.execute { indexExists("recogito") }.await
    if (!response.isExists) {
      Logger.info("Creating index 'recogito'")
      client.execute { create index "recogito" }.await      
    }
    client
  }
                    
  "ElasticSearch" should {
   
     "properly initialize mappings from config file" in {       
       val client = getClient()
       
       // client.execute { create index "recogito" mappings JsonDocumentSource(annotationMapping) }.await      
       
       val response = client.execute { getMapping("recogito" / "annotation") }.await
       Logger.info(response.getMappings.toString)
       
       response.getMappings.keys.contains("annotation") must equalTo(true)       
     }
    
  }
  
}
