package transform.iiif.api.presentation

import helpers.FileHelper
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import transform.iiif.api.PlainLiteral

class ManifestSpec extends PlaySpec with FileHelper {

  "The manifest file" should {
    
    "parse succesfully" in {
      val json = loadJSON("transform/iiif/api/presentation/example_manifest.json")
      val result = Json.fromJson[Manifest](json)
      
      result.isSuccess mustBe true
      
      val manifest = result.get
      manifest.sequences.size mustBe 1
      
      val sequence = manifest.sequences.head
      sequence.canvases.size mustBe 1
      
      val canvas = sequence.canvases.head
      canvas.label mustBe PlainLiteral("p. 1")
      canvas.images.size mustBe 1
      
      val image = canvas.images.head
      image.height mustBe 2000
      image.width mustBe 1500
      image.service mustBe "http://example.org/images/book1-page1/info.json"
    }
  
  }
  
}
