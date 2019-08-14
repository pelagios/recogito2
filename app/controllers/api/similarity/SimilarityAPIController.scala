package controllers.api.similarity

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseOptAuthController, Security, HasPrettyPrintJSON}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.ControllerComponents 
import scala.concurrent.{ExecutionContext, Future}
import services.RuntimeAccessLevel
import services.document.{DocumentService, ExtendedDocumentMetadata}
import services.generated.tables.records.DocumentRecord
import services.user.UserService
import services.similarity.{Similarity, SimilarityService}

@Singleton
class SimilarityAPIController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val similarities: SimilarityService,
  val users: UserService,
  val silhouette: Silhouette[Security.Env],
  implicit val ctx: ExecutionContext
) extends BaseOptAuthController(components, config, documents, users) with HasPrettyPrintJSON {

  implicit val similarDocumentWrites: Writes[(DocumentRecord, RuntimeAccessLevel, Similarity)] = (
    (JsPath \ "document_id").write[String] and
    (JsPath \ "author").writeNullable[String] and
    (JsPath \ "title").write[String] and 
    (JsPath \ "owner").write[String] and
    (JsPath \ "similarity").write[JsValue]
  )(t => (
      t._1.getId, 
      Option(t._1.getAuthor),
      t._1.getTitle,
      t._1.getOwner,
      Json.obj(
        "title" -> t._3.jaroWinkler, 
        "entities" -> t._3.jaccard)
  ))

  implicit val similarityAPIResponseWrites: Writes[(ExtendedDocumentMetadata, Seq[(DocumentRecord, RuntimeAccessLevel, Similarity)])] = (
    (JsPath \ "document_id").write[String] and
    (JsPath \ "author").writeNullable[String] and
    (JsPath \ "title").write[String] and 
    (JsPath \ "owner").write[String] and 
    (JsPath \ "similar").write[Seq[(DocumentRecord, RuntimeAccessLevel, Similarity)]]
  )(t => (
      t._1.id,
      t._1.author,
      t._1.title,
      t._1.ownerName,
      t._2
  ))

  def getSimilar(docId: String) = silhouette.UserAwareAction.async { implicit request => 
    documentResponse(docId, request.identity, { case (doc, accesslevel) => 
      if (accesslevel.canReadData) {
        similarities.findSimilar(docId, request.identity.map(_.username)).map { response => 
          jsonOk(Json.toJson((doc, response)))
        }
      } else {
        Future.successful(Forbidden)
      }
    })
  }
  
}
