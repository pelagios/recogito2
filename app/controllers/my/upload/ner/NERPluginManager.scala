package controllers.my.upload.ner

import play.api.Logger
import java.io.File
import org.apache.commons.io.FileUtils
import scala.collection.JavaConverters._
import java.net.URLClassLoader
import java.util.ServiceLoader
import org.pelagios.recogito.sdk.ner.NERPlugin

object NERPluginManager {
  
  private val PLUGIN_DIR = new File("plugins")
  
  val plugins = {
    Logger.info("Loading available NER plugins...")
    val serviceLoader = ServiceLoader.load(classOf[NERPlugin], Thread.currentThread().getContextClassLoader)
    
    val plugins = serviceLoader.asScala.toSeq
    Logger.info("Successfully loaded " + plugins.size + " NER plugins:")
    plugins.foreach(plugin => Logger.info("  " + plugin.getName))
    
    plugins       
  }
  
  def getDefaultNER() = plugins.head
  
}