package controllers.my.directory.list.document

import controllers.my.directory._
import controllers.my.directory.list.DirectoryController
import java.util.UUID
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request}
import scala.concurrent.Future
import services.{ContentType, Page}

trait SortByIndex { self: DirectoryController =>

  private def sortByIndexProperty(
    docIds: Seq[String], 
    sort: Sorting,
    offset: Int, 
    size: Int
  ): Future[Seq[String]] = {
    if (docIds.isEmpty) { // Just being defensive here
      Future.successful(docIds)
    } else {
      sort.sortBy match {
        case "last_edit_at" => contributions.sortDocsByLastModifiedAt(docIds, sort.order, offset, size)
        case "last_edit_by" => contributions.sortDocsByLastModifiedBy(docIds, sort.order, offset, size)
        case "annotations" => annotations.sortDocsByAnnotationCount(docIds, sort.order, offset, size)
        case _ => Future.successful(docIds)
      }
    }
  }

  protected def getMyDocumentsSortedByIndex(
    username: String,
    folder: Option[UUID],
    offset: Int, 
    size: Int,
    config: PresentationConfig
  )(implicit request: Request[AnyContent]) = {
    val startTime = System.currentTimeMillis

    val f = for {
      allIds <- documents.listIds(folder, username)
      sortedIds <- sortByIndexProperty(allIds, config.sort.get, offset, size)
      documents <- documents.getDocumentsById(sortedIds)
      indexProperties <- fetchIndexProperties(username, sortedIds, config)      
    } yield (allIds, sortedIds, documents, indexProperties)

    f.map { case (allIds, sortedIds, documents, indexProperties) => 
      val dbResult = Page(System.currentTimeMillis - startTime, allIds.size, offset, size, documents)
      ConfiguredPresentation.forMyDocument(dbResult, Some(indexProperties.toMap), Some(config.columns))
    }
  }

  protected def getSharedDocumentsSortedByIndex(
    username: String,
    folder: Option[UUID],
    offset: Int, 
    size: Int,
    config: PresentationConfig
  )(implicit request: Request[AnyContent]) = {
    val startTime = System.currentTimeMillis

    val f = for {
      allIds <- documents.listIdsSharedWithMe(username, folder)
      sortedIds <- sortByIndexProperty(allIds, config.sort.get, offset, size)
      documents <- documents.getDocsSharedWithMeById(sortedIds, username)
      indexProperties <- fetchIndexProperties(username, sortedIds, config)
    } yield (allIds, sortedIds, documents, indexProperties)

    f.map { case (allIds, sortedIds, documents, indexProperties) =>
      val dbResult = Page(System.currentTimeMillis - startTime, allIds.size, offset, size, documents)
      ConfiguredPresentation.forSharedDocument(dbResult, Some(indexProperties.toMap), Some(config.columns))
    }
  }

  protected def getAccessibleDocumentsSortedByIndex(
    owner: String, 
    folder: Option[UUID],
    loggedIn: Option[String],
    offset: Int,
    size: Int,
    config: PresentationConfig
  )(implicit request: Request[AnyContent]) = {
    val startTime = System.currentTimeMillis

    val f = for {
      allIds <- documents.listAccessibleIds(owner, folder, loggedIn)
      sortedIds <- sortByIndexProperty(allIds, config.sort.get, offset, size)
      documents <- documents.getDocumentsById(sortedIds)
      indexProperties <- fetchIndexProperties(owner, sortedIds, config)
    } yield (allIds, sortedIds, documents, indexProperties)

    f.map { case (allIds, sortedIds, documents, indexProperties) =>
      // TODO fetch with sharing permissions, if any
      val dbResult = Page(System.currentTimeMillis - startTime, allIds.size, offset, size, documents)
      ConfiguredPresentation.forMyDocument(dbResult, Some(indexProperties.toMap), Some(config.columns))
    }
  }

}