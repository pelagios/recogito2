package controllers.my.directory.list.document

import controllers.my.directory._
import controllers.my.directory.list.DirectoryController
import java.util.UUID
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request}
import scala.concurrent.{ExecutionContext, Future}
import services.{Page, SortOrder}
import services.document.DocumentService
import services.generated.tables.records.DocumentRecord

trait SortByDB { self: DirectoryController =>

  /** Boilerplate to fetch documents sorted via a DB property */
  protected def documentsByDB[T <: Product](
    config: Option[PresentationConfig],
    fn: () => Future[Page[T]]
  )(implicit ctx: ExecutionContext) = for {
    documents <- fn()
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
    folder: Option[UUID],
    offset: Int, 
    size: Int, 
    config: Option[PresentationConfig]
  )(implicit request: Request[AnyContent]) = {
    documentsByDB(
      config, 
      () => documents.listByOwnerAndFolder(
              username, folder, offset, size,
              config.flatMap(_.sort.map(_.sortBy)),
              config.flatMap(_.sort.map(_.order)))
    ).map { case (documents, indexProperties) =>
      ConfiguredPresentation.forMyDocument(documents, indexProperties.map(_.toMap), config.map(_.columns))
    }
  }

  /** 'Shared with me' documents, sorted by a DB property **/
  protected def getSharedDocumentsSortedByDB(
    username: String,
    folder: Option[UUID],
    offset: Int, 
    size: Int, 
    config: Option[PresentationConfig]
  )(implicit request: Request[AnyContent]) = {   
    documentsByDB(
      config, 
      () => documents.listDocumentsSharedWithMe(
              username, folder, offset, size,
              config.flatMap(_.sort.map(_.sortBy)), 
              config.flatMap(_.sort.map(_.order)))
    ).map { case (sharedDocs, indexProperties) =>
      ConfiguredPresentation.forSharedDocument(sharedDocs, indexProperties.map(_.toMap), config.map(_.columns))
    }
  }

  protected def getAccessibleDocumentsSortedByDB(
    owner: String, 
    folder: Option[UUID],
    loggedIn: Option[String],
    offset: Int,
    size: Int,
    config: Option[PresentationConfig]
  )(implicit request: Request[AnyContent]) = {
    documentsByDB(
      config,
      () => documents.listAccessibleDocuments(
              owner, folder, loggedIn, offset, size,
              config.flatMap(_.sort.map(_.sortBy)),
              config.flatMap(_.sort.map(_.order)))
    ).map { case (documents, indexProperties) =>
      ConfiguredPresentation.forAccessibleDocument(documents, indexProperties.map(_.toMap), config.map(_.columns))
    }
  }

}