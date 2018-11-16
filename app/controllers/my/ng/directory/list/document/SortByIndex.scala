package controllers.my.ng.directory.list.document

import controllers.my.ng.directory.list.DirectoryController
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request}
import scala.concurrent.Future
import services.Page

trait SortByIndex { self: DirectoryController =>

  private def sortByIndexProperty(
    docIds: Seq[String], 
    sort: Sorting,
    offset: Int, 
    size: Int
  ): Future[Seq[String]] = sort.sortBy match {
    case "last_edit_at" => contributions.sortDocsByLastModifiedAt(docIds, sort.order, offset, size)
    case "last_edit_by" => contributions.sortDocsByLastModifiedBy(docIds, sort.order, offset, size)
    case "annotations" => annotations.sortDocsByAnnotationCount(docIds, sort.order, offset, size)
    case _ => Future.successful(docIds)
  }

  protected def getMyDocumentsSortedByIndex(
    username: String,
    offset: Int, 
    size: Int,
    config: PresentationConfig
  )(implicit request: Request[AnyContent]) = {
    val startTime = System.currentTimeMillis

    val f = for {
      allIds <- documents.listAllIdsByOwner(username)
      sortedIds <- sortByIndexProperty(allIds, config.sort.get, offset, size)
      docsWithParts <- documents.findByIdsWithParts(sortedIds)
      indexProperties <- fetchIndexProperties(sortedIds, config)      
    } yield (allIds, sortedIds, docsWithParts, indexProperties)

    f.map { case (allIds, sortedIds, docsWithParts, indexProperties) => 
      val dbResult = Page(System.currentTimeMillis - startTime, allIds.size, offset, size, docsWithParts)
      val interleaved = ConfiguredPresentation.forMyDocument(dbResult, Some(indexProperties.toMap), Some(config.columns))
      jsonOk(Json.toJson(interleaved))
    }
  }

  protected def getSharedDocumentsSortedByIndex(
    username: String,
    offset: Int, 
    size: Int,
    config: PresentationConfig
  )(implicit request: Request[AnyContent]) = {
    val startTime = System.currentTimeMillis

    val f = for {
      allIds <- documents.listAllIdsSharedWith(username)
      sortedIds <- sortByIndexProperty(allIds, config.sort.get, offset, size)
      documents <- documents.findByIdsWithPartsAndSharingPolicy(sortedIds, username)
      indexProperties <- fetchIndexProperties(sortedIds, config)
    } yield (allIds, sortedIds, documents, indexProperties)

    f.map { case (allIds, sortedIds, documents, indexProperties) =>
      val dbResult = Page(System.currentTimeMillis - startTime, allIds.size, offset, size, documents)
      val interleaved = ConfiguredPresentation.forSharedDocument(dbResult, Some(indexProperties.toMap), Some(config.columns))
      jsonOk(Json.toJson(interleaved))
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
      val interleaved = ConfiguredPresentation.forMyDocument(dbResult, Some(indexProperties.toMap), Some(config.columns))
      jsonOk(Json.toJson(interleaved))
    }
  }

}