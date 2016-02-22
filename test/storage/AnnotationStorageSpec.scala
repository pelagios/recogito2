package storage

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
          // .put("path.home", "es-root/")

    val client = ElasticClient.local(settings.build)
    
    val response = client.execute { indexExists("recogito") }.await
    if (response.isExists) {
      Logger.info("Index 'recogito' exists - dropping")
      client.execute { delete index "recogito" }.await
      Logger.info("done.")
    }
 
    client
  }
                    
  "ElasticSearch" should {
   
     "properly initialize mappings from config file" in {       
       val client = getClient()
       
       Logger.info("Creating annotation mapping")
       
       // client.execute { create index "recogito" mappings JsonDocumentSource(annotationMapping) }.await      
       val foo = client.admin.indices().prepareCreate("recogito")
       foo.addMapping("annotation", annotationMapping)
       foo.execute().actionGet()
       
       Logger.info("OK - checking if its there")
       
       val response = client.execute { getMapping("recogito" / "annotation") }.await
       Logger.info(response.getMappings.toString)
       
       response.getMappings.get("recogito").keys.contains("annotation") must equalTo(true)       
     }
    
  }
  
}
