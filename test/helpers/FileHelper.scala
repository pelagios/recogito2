package helpers

import java.io.File
import play.api.libs.json.Json
import scala.io.Source

trait FileHelper {
  
  private val RESOURCES_PATH = "test/resources"
  
  def loadJSON(relativePath: String) = {
    val path = new File(RESOURCES_PATH, relativePath)
    val lines = Source.fromFile(path).getLines().mkString("\n")
    Json.parse(lines)
  }
  
}