package controllers.help

import controllers.HasVisitLogging
import javax.inject.{Inject, Singleton}
import services.visit.VisitService
import org.webjars.play.WebJarsUtil
import play.api.{Configuration, Environment}
import play.api.mvc.{Action, AbstractController, ControllerComponents, RequestHeader}
import play.twirl.api.HtmlFormat
import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.util.Try
import services.entity.{AuthorityFileService, EntityType}

@Singleton
class HelpController @Inject() (
  val authorities: AuthorityFileService,
  val components: ControllerComponents,
  val config: Configuration,
  val env: Environment,
  implicit val ctx: ExecutionContext,
  implicit val visits: VisitService,
  implicit val webjars: WebJarsUtil
) extends AbstractController(components) with HasVisitLogging {

  private val adminEmail = config.get[String]("admin.email")
  private val imprint =
    Try(Source.fromFile(env.getFile("conf/imprint"))).toOption.map { _.getLines.mkString("\n") }

  private def result(template: HtmlFormat.Appendable)(implicit request: RequestHeader) = {
    logPageView()
    Ok(template)
  }

  def localizedTutorial(lang: String) = Action { implicit request =>
    lang.toUpperCase match {
      case "DE" => result(views.html.help.tutorial.tutorial_de())
      case "ES" => result(views.html.help.tutorial.tutorial_es())
      case "FA" => result(views.html.help.tutorial.tutorial_fa())
      case "FR" => result(views.html.help.tutorial.tutorial_fr())
      case "IT" => result(views.html.help.tutorial.tutorial_it())
      case "NL" => result(views.html.help.tutorial.tutorial_nl())
      case "TR" => result(views.html.help.tutorial.tutorial_tr())
      case _ => NotFound(views.html.error404())
    }
  }

  def faq = Action.async { implicit request => 
    authorities.listAll(Some(EntityType.PLACE)).map { gazetteers =>
      result(views.html.help.faq(gazetteers)) 
    }
  }

  def index = Action { implicit request => result(views.html.help.index()) }

  def about        = Action { implicit request => result(views.html.help.general.about(imprint, adminEmail)) }
  def privacy      = Action { implicit request => result(views.html.help.general.privacy(adminEmail)) }
  def relations    = Action { implicit request => result(views.html.help.relations()) }
  def sharingLinks = Action { implicit request => result(views.html.help.sharing_links()) }
  def terms        = Action { implicit request => result(views.html.help.general.terms()) }
  def tutorial     = Action { implicit request => result(views.html.help.tutorial.tutorial()) }
  def workspace    = Action { implicit request => result(views.html.help.workspace()) }

  def swaggerUi = Action {
    Redirect(url = "/webjars/swagger-ui/2.2.0/index.html", queryString = Map("url" -> Seq("/swagger.json")))
  }

}
