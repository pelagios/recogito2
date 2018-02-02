package controllers.document.stats

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseOptAuthController, Security, HasVisitLogging, HasPrettyPrintJSON}
import javax.inject.{Inject, Singleton}
import services.annotation.stats.AnnotationStatsService
import services.document.DocumentService
import services.user.UserService
import services.user.Roles._
import services.visit.VisitService
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.ControllerComponents
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StatsController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val statsService: AnnotationStatsService,
  val users: UserService,
  val silhouette: Silhouette[Security.Env],
  implicit val visitService: VisitService,
  implicit val webjars: WebJarsUtil,
  implicit val ctx: ExecutionContext
) extends BaseOptAuthController(components, config, documents, users) with HasVisitLogging with HasPrettyPrintJSON {
  
  implicit val tuple2Writes: Writes[Tuple2[String, Long]] = (
    (JsPath \ "value").write[String] and
    (JsPath \ "count").write[Long]
  )(t => (t._1, t._2))
  
  /** TODO this view should be available without login, if the document is set to public **/
  def showDocumentStats(documentId: String, tab: Option[String]) = silhouette.UserAwareAction.async { implicit request =>
    documentReadResponse(documentId, request.identity,  { case (doc, accesslevel) =>
      logDocumentView(doc.document, None, accesslevel)      
      tab.map(_.toLowerCase) match {
        case Some(t) if t == "activity" =>
          Future.successful(Ok(views.html.document.stats.activity(doc, request.identity, accesslevel)))
          
        case Some(t) if t == "entities" =>
          Future.successful(Ok(views.html.document.stats.entities(doc, request.identity, accesslevel)))
          
        case Some(t) if t == "tags" =>
          Future.successful(Ok(views.html.document.stats.tags(doc, request.identity, accesslevel)))
          
        case _ =>
          Future.successful(Ok(views.html.document.stats.activity(doc, request.identity, accesslevel)))
      }
    })
  }
  
  def getTagStats(documentId: String) = silhouette.UserAwareAction.async { implicit request =>
    statsService.getTagStats(documentId).map { buckets =>
      jsonOk(Json.toJson(buckets))
    }
  }

}
