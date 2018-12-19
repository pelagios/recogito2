package controllers.document.stats

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseOptAuthController, Security, HasVisitLogging, HasPrettyPrintJSON}
import java.io.{ByteArrayOutputStream, PrintWriter}
import javax.inject.{Inject, Singleton}
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.CsvConfiguration.{Header, QuotePolicy}
import kantan.csv.engine.commons._
import services.annotation.AnnotationService
import services.document.DocumentService
import services.user.UserService
import services.user.Roles._
import services.visit.VisitService
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.{AnyContent, Request, Result, ControllerComponents}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.i18n.I18nSupport
import plugins.PluginRegistry
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatsController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val annotations: AnnotationService,
  val users: UserService,
  val silhouette: Silhouette[Security.Env],
  implicit val visitService: VisitService,
  implicit val webjars: WebJarsUtil,
  implicit val ctx: ExecutionContext
) extends BaseOptAuthController(components, config, documents, users) 
    with HasVisitLogging 
    with HasPrettyPrintJSON 
    with I18nSupport {
  
  private val CSV_CONFIG = CsvConfiguration(',', '"', QuotePolicy.WhenNeeded, Header.None)
  
  // TODO DUMMY!
  private val plugins = PluginRegistry.listConfigs("js.stats")
  
  implicit val tuple2Writes: Writes[Tuple2[String, Long]] = (
    (JsPath \ "value").write[String] and
    (JsPath \ "count").write[Long]
  )(t => (t._1, t._2))
  
  private def toCSV(stats: Seq[(String, Long)]): String = {
    val out = new ByteArrayOutputStream()
    val writer = out.asCsvWriter[(String, Long)](CSV_CONFIG)
    stats.foreach(writer.write(_))
    writer.close()
    new String(out.toByteArray, "UTF-8")
  }
  
  def showDocumentStats(documentId: String, tab: Option[String]) = silhouette.UserAwareAction.async { implicit request =>
    documentReadResponse(documentId, request.identity,  { case (doc, accesslevel) =>
      logDocumentView(doc.document, None, accesslevel)      
      tab.map(_.toLowerCase) match {
        case Some(t) if t == "activity" =>
          Future.successful(Ok(views.html.document.stats.activity(doc, request.identity, accesslevel)))
          
        case Some(t) if t == "entities" =>
          Future.successful(Ok(views.html.document.stats.entities(doc, request.identity, accesslevel)))
          
        case Some(t) if t == "tags" =>
          Future.successful(Ok(views.html.document.stats.tags(doc, request.identity, accesslevel, plugins)))
          
        case _ =>
          Future.successful(Ok(views.html.document.stats.activity(doc, request.identity, accesslevel)))
      }
    })
  }
  
  private def getTags(documentId: String)(action: (Seq[(String, Long)], Request[AnyContent]) => Result) =
    silhouette.UserAwareAction.async { implicit request =>
      documentReadResponse(documentId, request.identity,  { case (doc, accesslevel) =>
          annotations.getTagStats(documentId).map { buckets =>
            action(buckets, request.request)
          }
        }
      )
    }
  
  def getTagsAsJSON(documentId: String) = getTags(documentId) { case (buckets, request) =>
    jsonOk(Json.toJson(buckets))(request)
  }
  
  def getTagsAsCSV(documentId: String) = getTags(documentId) { case(buckets, request) =>
    Ok(toCSV(buckets)).withHeaders(CONTENT_DISPOSITION -> { s"attachment; filename=${documentId}_tags.csv" })
  }

}
