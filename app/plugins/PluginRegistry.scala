package plugins

import com.typesafe.config.{Config, ConfigFactory}
import java.io.File
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

object PluginRegistry {
  
  private val PLUGIN_DIR = new File("plugins") 

  Logger.info("Loading plugin configurations:")
  
  private val configs: Seq[(Config, File)] = 
    findFilesRecursive("plugin.conf", PLUGIN_DIR)
      .map { file => 
        val config = ConfigFactory.parseFile(file) 
        val dir = file.getParentFile
        (config, dir)
      }
  
  configs.foreach { case (c, dir) =>
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
          
  def listConfigs(extensionPoint: String): Seq[Config] =
    configs
      .filter(_._1.getString("extends").equalsIgnoreCase(extensionPoint))
      .map(_._1)

  def loadPlugin(extensionPoint: String, className: String)(implicit ctx: ExecutionContext): Future[Option[String]] =
    scala.concurrent.blocking {
      Future {
        configs.find { case (config, dir) => 
          config.getString("extends").equalsIgnoreCase(extensionPoint) &&
          config.getString("className").equalsIgnoreCase(className)
        } map { case (config, dir) => 
          val pluginFile = new File(dir, "plugin.js")
          Source.fromFile(pluginFile).getLines.mkString("\n")
        }
      }
    }
  
}