package controllers.my.directory.list

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import controllers.my.directory._
import controllers.my.directory.list.document._
import controllers.my.directory.list.folder._
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, ControllerComponents, Request, Result}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import services.Page
import services.annotation.AnnotationService
import services.annotation.stats.StatusRatio
import services.contribution.ContributionService
import services.document.DocumentService
import services.folder.{Breadcrumb, FolderService}
import services.generated.tables.records.FolderRecord
import services.user.UserService

@Singleton
class DirectoryController @Inject() (
  val annotations: AnnotationService,
  val contributions: ContributionService,
  val components: ControllerComponents,
  val documents: DocumentService,
  val folders: FolderService,
  val silhouette: Silhouette[Security.Env],
  val users: UserService,
  val config: Configuration,
  implicit val ctx: ExecutionContext
) extends BaseController(components, config, users)
    with SortByDB
    with SortByIndex
    with FolderUtils
    with HasPrettyPrintJSON {

  // Document properties derived from the index
  private val INDEX_SORT_PROPERTIES = 
    Seq("last_edit_at", "last_edit_by", "annotations")

  private def isSortingByIndex(config: Option[PresentationConfig]) =
    config
      .flatMap(_.sort.map(_.sortBy))
      .map(field => INDEX_SORT_PROPERTIES.contains(field.toLowerCase))
      .getOrElse(false)

  /** Takes a list of document IDs and, for each, fetches last edit and number of annotations from the index **/
  protected def fetchIndexProperties(currentUser: String, docIds: Seq[String], config: PresentationConfig) = {
    // Helper that wraps the common bits: conditional execution, sequence-ing, mapping to (id -> result) tuple
    def fetchIfRequested[T](field: String*)(fn: String => Future[T]) =
      if (config.hasAnyColumn(field))
        Future.sequence(docIds.map(id => fn(id).map((id, _)))) 
      else 
        Future.successful(Seq())

    val fLastEdits = fetchIfRequested("last_edit_at", "last_edit_by") { id =>
      contributions.getLastContribution(id)
    }

    val fMyLastEdits = fetchIfRequested("my_last_edit_at") { id => 
      contributions.getMyLastContribution(currentUser, id)
    }

    val fAnnotationCounts = fetchIfRequested("annotations") { id =>
      annotations.countByDocId(id)
    }

    val fMyAnnotationCounts = fetchIfRequested("my_annotations") { id => 
      annotations.countMineByDocId(currentUser, id)
    }

    val fStatusRatios =
      if (config.hasColumn("status_ratio")) annotations.getStatusRatios(docIds)
      else Future.successful(Map.empty[String, StatusRatio])
 
    val f = for {
      lastEdits <- fLastEdits
      myLastEdits <- fMyLastEdits
      annotationCounts <- fAnnotationCounts
      myAnnotationCounts <- fMyAnnotationCounts
      statusRatios <- fStatusRatios
    } yield (lastEdits.toMap, myLastEdits.toMap, annotationCounts.toMap, myAnnotationCounts.toMap, statusRatios)   
    
    f.map { case (lastEdits, myLastEdits, annotationsPerDoc, myAnnotationsPerDoc, statusRatios) =>
      docIds.map { id =>
        val lastEdit = lastEdits.find(_._1 == id).flatMap(_._2)
        val myLastEdit = myLastEdits.find(_._1 == id).flatMap(_._2)
        val annotations = annotationsPerDoc.find(_._1 == id).map(_._2).getOrElse(0l)
        val myAnnotations = myAnnotationsPerDoc.find(_._1 == id).map(_._2).getOrElse(0l)

        val indexProps = IndexDerivedProperties(
          lastEdit.map(_.madeAt),
          lastEdit.map(_.madeBy),
          myLastEdit.map(_.madeAt),
          Some(annotations),
          Some(myAnnotations),
          statusRatios.get(id))

        (id, indexProps)
      }
    }
  }

  /** Common boilerplate code **/
  private def getDocumentList(
    username: String, offset: Int, size: Int,
    folderId: Option[UUID],
    onSortByDB   : (String, Option[UUID], Int, Int, Option[PresentationConfig]) => Future[Page[ConfiguredPresentation]],
    onSortByIndex: (String, Option[UUID], Int, Int, PresentationConfig) => Future[Page[ConfiguredPresentation]]
  )(implicit request: Request[AnyContent]) = {
    val config = request.body.asJson.flatMap(json => 
      Try(Json.fromJson[PresentationConfig](json).get).toOption)

    if (isSortingByIndex(config))
      onSortByIndex(username, folderId, offset, size, config.get)
    else 
      onSortByDB(username, folderId, offset, size, config)
  }

  def getMyDirectory(offset: Int, size: Int, folderId: UUID) =
    silhouette.SecuredAction.async { implicit request =>
      val fReadme = getReadme(Option(folderId), request.identity)
      val fBreadcrumbs = getBreadcrumbs(Option(folderId))
      val fDirectories = folders.listFolders(request.identity.username, offset, size, Option(folderId))     
      
      def fDocuments(folders: Page[(FolderRecord, Int)]) = {
        val shiftedOffset = Math.max(0l, offset - folders.total)
        val shiftedSize = size - folders.items.size

        if (shiftedOffset < 0)
          // Result is folders only
          Future.successful(Page.empty[ConfiguredPresentation])
        else
          getDocumentList(
            request.identity.username, 
            shiftedOffset.toInt, 
            shiftedSize, 
            Option(folderId),
            getMyDocumentsSortedByDB, 
            getMyDocumentsSortedByIndex
          )
      }

      val f = for {
        readme <- fReadme
        breadcrumbs <- fBreadcrumbs
        directories <- fDirectories
        documents <- fDocuments(directories)
      } yield (readme, breadcrumbs, directories.map(t => FolderItem(t._1, t._2)), documents)

      f.map { case (readme, breadcrumbs, directories, documents) => 
        val result = DirectoryPage.build(readme, breadcrumbs, directories, documents)
        jsonOk(Json.toJson(result))
      }
    }

  def getSharedWithMe(offset: Int, size: Int, folderId: UUID) = 
    silhouette.SecuredAction.async { implicit request => 
      val fBreadcrumbs = Option(folderId).map { id => 
          folders.getSharedWithMeBreadcrumbTrail(request.identity.username, id)
        } getOrElse { Future.successful(Seq.empty[Breadcrumb]) }

      val fDirectories = folders.listFoldersSharedWithMe(request.identity.username, Option(folderId))
    
      val fDocuments = getDocumentList(
        request.identity.username, 
        offset, 
        size, 
        Option(folderId),
        getSharedDocumentsSortedByDB, 
        getSharedDocumentsSortedByIndex
      )

      val f = for {
        breadcrumbs <- fBreadcrumbs
        directories <- fDirectories
        documents <- fDocuments
      } yield (
        breadcrumbs,
        directories.map(t => FolderItem(t._1, t._3, Some(t._2))), 
        documents
      )

      f.map { case (breadcrumbs, directories, documents) => 
        // TODO readme

        val result = DirectoryPage.build(
          None, // Readme
          breadcrumbs,
          directories,
          documents
        )

        jsonOk(Json.toJson(result))
      }
    }

  def getAccessibleDocuments(fromOwner: String, offset: Int, size: Int, folderId: UUID) =
    silhouette.UserAwareAction.async { implicit request =>
      val loggedIn = request.identity.map(_.username)

      users.findByUsernameIgnoreCase(fromOwner).flatMap(_ match { 
        case Some(owner) => 
          val fBreadcrumbs = Option(folderId).map { id => 
              folders.getAccessibleDocsBreadcrumbTrail(owner.username, loggedIn, id)
            } getOrElse { Future.successful(Seq.empty[Breadcrumb]) }

          val fDirectories = folders.listAccessibleFolders(owner.username, loggedIn, Option(folderId))

          val config = request.body.asJson.flatMap(json => 
            Try(Json.fromJson[PresentationConfig](json).get).toOption)

          val fDocuments =
            if (isSortingByIndex(config))
              getAccessibleDocumentsSortedByIndex(owner.username, Option(folderId), loggedIn, offset, size, config.get)
            else 
              getAccessibleDocumentsSortedByDB(owner.username, Option(folderId), loggedIn, offset, size, config)

          val f = for {
            breadcrumbs <- fBreadcrumbs
            directories <- fDirectories
            documents <- fDocuments
          } yield (
            breadcrumbs,
            directories.map(t => FolderItem(t._1, 0, t._2)), 
            documents)

          f.map { case (breadcrumbs, directories, documents) => 
            // Only expose readme if there are shared documents
            val readme = 
              if (documents.total > 0) owner.readme else None
              
            jsonOk(Json.toJson(DirectoryPage.build(
              readme,
              breadcrumbs,
              directories, 
              documents)))
          }

        case None =>
          Future.successful(NotFound)
      })
    }

}
