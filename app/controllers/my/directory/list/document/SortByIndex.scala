package controllers.my.directory.list.document

import controllers.my.directory.list.DirectoryController
import java.util.UUID
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request}
import scala.concurrent.Future
import services.{ContentType, Page}
import services.document.SharedDocument

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
      allIds <- documents.listIdsByOwnerInFolder(username, folder)
      sortedIds <- sortByIndexProperty(allIds, config.sort.get, offset, size)
      docsWithParts <- documents.findByIdsWithParts(sortedIds)
      indexProperties <- fetchIndexProperties(sortedIds, config)      
    } yield (allIds, sortedIds, docsWithParts, indexProperties)

    f.map { case (allIds, sortedIds, docsWithParts, indexProperties) => 
      val dbResult = Page(System.currentTimeMillis - startTime, allIds.size, offset, size, docsWithParts)
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
      documents <- documents.findByIdsWithPartsAndSharingPolicy(sortedIds, username)
      indexProperties <- fetchIndexProperties(sortedIds, config)
    } yield (allIds, sortedIds, documents, indexProperties)

    f.map { case (allIds, sortedIds, documents, indexProperties) =>
      val sharedDocs = documents.map { case (document, policy, fileparts) => 
        SharedDocument(
          document, 
          policy, 
          fileparts.size, 
          fileparts.flatMap(p => ContentType.withName(p.getContentType)).distinct)
      }

      val dbResult = Page(System.currentTimeMillis - startTime, allIds.size, offset, size, sharedDocs)
      ConfiguredPresentation.forSharedDocument(dbResult, Some(indexProperties.toMap), Some(config.columns))
    }
  }

  protected def getAccessibleDocumentsSortedByIndex(
    owner: String, 
    loggedIn: Option[String],
    offset: Int,
    size: Int,
    config: PresentationConfig
  )(implicit request: Request[AnyContent]) = {
    val startTime = System.currentTimeMillis

    val f = for {
      allIds <- documents.listAllAccessibleIds(owner, loggedIn)
      sortedIds <- sortByIndexProperty(allIds, config.sort.get, offset, size)
      documents <- documents.findByIdsWithParts(sortedIds)
      indexProperties <- fetchIndexProperties(sortedIds, config)
    } yield (allIds, sortedIds, documents, indexProperties)

    f.map { case (allIds, sortedIds, documents, indexProperties) =>
      // TODO fetch with sharing permissions, if any
      val dbResult = Page(System.currentTimeMillis - startTime, allIds.size, offset, size, documents)
      ConfiguredPresentation.forMyDocument(dbResult, Some(indexProperties.toMap), Some(config.columns))
    }
  }

}