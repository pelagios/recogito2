package controllers.api

import controllers.{BaseOptAuthController, HasPrettyPrintJSON}
import javax.inject.{Inject, Singleton}
import org.pelagios.recogito.sdk.ner.NERPlugin
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.ControllerComponents
import plugins.PluginRegistry
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import services.document.DocumentService
import services.user.UserService
import transform.ner.NERPluginManager

@Singleton
class PluginAPIController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val users: UserService,
  implicit val ctx: ExecutionContext
) extends BaseOptAuthController(components, config, documents, users) with HasPrettyPrintJSON {

  implicit val pluginWrites: Writes[NERPlugin] = (
    (JsPath \ "identifier").write[String] and
    (JsPath \ "name").write[String] and
    (JsPath \ "languages").write[Seq[String]] and
    (JsPath \ "organization").write[String] and
    (JsPath \ "description").write[String] and 
    (JsPath \ "version").write[String]
  )(p => (
     p.getClass.getName,
     p.getName,
     p.getSupportedLanguages.asScala,
     p.getOrganization,
     p.getDescription,
     p.getVersion
  ))

  def listNERPlugins = Action { implicit request =>
    val plugins = NERPluginManager.plugins
    jsonOk(Json.toJson(plugins))
  }

  def loadPlugin(ext: String, id: String) = Action.async { implicit request =>
    PluginRegistry.loadPlugin(ext, id).map { _ match {
      case Some(js) => Ok(js).as("application/javascript")
      case None => NotFound 
    }}
  }

  def loadCSS(ext: String, id: String) = Action.async { implicit request => 
    PluginRegistry.loadCSS(ext, id).map { _ match {
      case Some(css) => Ok(css).as("text/css")
      case None => NotFound
    }}
  }

}