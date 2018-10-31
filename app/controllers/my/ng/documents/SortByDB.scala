package controllers.my.ng.documents

import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request}
import scala.concurrent.{ExecutionContext, Future}
import services.{Page, SortOrder}
import services.document.DocumentService
import services.generated.tables.records.DocumentRecord

trait SortByDB { self: DocumentInfoController =>

  /** Boilerplate to fetch documents sorted via a DB property */
  protected def documentsByDB[T <: Product](
    username: String,
    offset: Int, 
    size: Int, 
    config: Option[PresentationConfig],
    fn: (String, Int, Int, Option[String], Option[SortOrder]) => Future[Page[T]]
  )(implicit ctx: ExecutionContext) = for {
    documents <- fn(username, offset, size, 
      config.flatMap(_.sort.map(_.sortBy)),
      config.flatMap(_.sort.map(_.order)))

    indexProperties <- config match {
      case Some(c) => 
        val ids = documents.items.map(_.productElement(0).asInstanceOf[DocumentRecord].getId)
        fetchIndexProperties(ids, c).map(Some(_))

      case None => Future.successful(None)
    }
  } yield (documents, indexProperties)

  /** My Documents, sorted by a DB property **/
  protected def getMyDocumentsSortedByDB(
    username: String,
    offset: Int, 
    size: Int, 
    config: Option[PresentationConfig]
  )(implicit request: Request[AnyContent]) = {
    documentsByDB(
      username, offset, size, config, documents.findByOwnerWithParts
    ).map { case (documents, indexProperties) =>
      val interleaved = ConfiguredPresentation.forMyDocument(documents, indexProperties.map(_.toMap), config.map(_.columns))
      jsonOk(Json.toJson(interleaved))
    }
  }

  /** 'Shared with me' documents, sorted by a DB property **/
  protected def getSharedDocumentsSortedByDB(
    username: String,
    offset: Int, 
    size: Int, 
    config: Option[PresentationConfig]
  )(implicit request: Request[AnyContent]) = {   
    documentsByDB(
      username, offset, size, config, documents.findSharedWithPart
    ).map { case (documents, indexProperties) =>
      val interleaved = ConfiguredPresentation.forSharedDocument(documents, indexProperties.map(_.toMap), config.map(_.columns))
      jsonOk(Json.toJson(interleaved))
    }
  }

}