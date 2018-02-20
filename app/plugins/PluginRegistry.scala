package plugins

import com.typesafe.config.{Config, ConfigFactory}
import java.io.File
import play.api.Logger

object PluginRegistry {
  
  private val PLUGIN_DIR = new File("plugins") 

  Logger.info("Loading plugin configurations:")
  
  private val configs: Seq[Config] = findFilesRecursive("plugin.conf", PLUGIN_DIR).map(ConfigFactory.parseFile(_))
  
  configs.foreach { c =>
    Logger.info(s"  ${c.getString("extends")}.${c.getString("title")}")
  }
  
  Logger.info(s"${configs.size} configurations found")
  
  /** Recursively walks a directory, looking for files with the given name **/
  private def findFilesRecursive(name: String, dir: File): Seq[File] = {
    val all = dir.listFiles
   
    val dirs = all.filter(_.isDirectory)
    val files = all.filter(_.isFile)
    
    val matchingFiles = files.filter(_.getName == name)
    
    matchingFiles ++ dirs.flatMap(dir => findFilesRecursive(name, dir))
  }
          
  def getPlugins(extensionPoint: String): Seq[Config] =
    configs.filter(c => c.getString("extends") == extensionPoint)
  
}