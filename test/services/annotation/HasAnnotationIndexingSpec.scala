package services.annotation

import java.util.UUID
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class HasAnnotationIndexingSpec extends Specification with HasAnnotationIndexing {
  
  import services.annotation.BackendAnnotation._
  
  "The helper" should {
    
    "properly remove relations" in {
      val json = Source.fromFile("test/resources/services/annotation/annotation-with-relation.json").getLines().mkString("\n")
      val result = Json.fromJson[Annotation](Json.parse(json))
      
      val annotation = result.get
      annotation.relations.size must equalTo(1)
      
      val unchanged = removeRelationsTo(annotation, UUID.randomUUID)
      unchanged.relations.size must equalTo(1)
      unchanged.relations must equalTo(annotation.relations)
      
      val changed = removeRelationsTo(annotation, UUID.fromString("a00566be-c51a-4831-a270-c700f06bc205"))
      changed.relations.size must equalTo(0)
    }
    
  }
  
}