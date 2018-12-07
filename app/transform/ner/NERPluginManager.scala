package transform.ner

import java.io.File
import java.util.ServiceLoader
import play.api.Logger
import org.pelagios.recogito.sdk.ner.NERPlugin
import scala.collection.JavaConverters._

object NERPluginManager {

  private val pluginsByClassname = {
    Logger.info("Loading available NER plugins...")
    val serviceLoader = ServiceLoader.load(classOf[NERPlugin], Thread.currentThread().getContextClassLoader)

    val plugins = serviceLoader.asScala.toSeq
    Logger.info("Successfully loaded " + plugins.size + " NER plugins:")

    plugins.map { plugin => 
      Logger.info("  " + plugin.getName)
      (plugin.getClass.getName -> plugin)
    }
  }

  val plugins = pluginsByClassname.map(_._2)

  /** I guess there's nothing smart we can do **/
  def getDefaultEngine(): NERPlugin = plugins.head

  def getEngine(className: String): Option[NERPlugin] = 
    pluginsByClassname.find(_._1 == className).map(_._2)

}
