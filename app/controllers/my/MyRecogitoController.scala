package controllers.my

import controllers.BaseController
import javax.inject.{ Inject, Singleton }
import models.{ Page, SortOrder }
import models.annotation.AnnotationService
import models.contribution.{ Contribution, ContributionService }
import models.user.{ User, UserService }
import models.document.DocumentService
import models.generated.tables.records.{ DocumentRecord, SharingPolicyRecord }
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.RequestHeader
import scala.concurrent.{ ExecutionContext, Future }
import storage.Uploads

@Singleton
class MyRecogitoController @Inject() (
    val annotations: AnnotationService,
    val contributions: ContributionService,
    val documents: DocumentService,
    val users: UserService,
    val uploads: Uploads,
    val config: Configuration,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarsUtil
  ) extends BaseController(config, users) {

  private val DEFAULT_DOCUMENTS_PER_PAGE = 10
  
  private val INDEX_SORT_PROPERTIES = Seq("last_modified_at", "last_modified_by", "annotations")
  
  private def isSortingByIndex(sortBy: String) =
    INDEX_SORT_PROPERTIES.contains(sortBy.toLowerCase)

  private def renderPublicProfile(usernameInPath: String, loggedInUser: Option[User])(implicit request: RequestHeader) = {
    val fOwnerWithRoles = users.findByUsernameIgnoreCase(usernameInPath)
    
    // TODO add pagination and sorting options
    
    val fDocs = documents.findAccessibleDocuments(usernameInPath, loggedInUser.map(_.username), 0, 100)
    
    val f = for {
      ownerWithRoles <- fOwnerWithRoles
      docs <- fDocs
    } yield (ownerWithRoles, docs)    

    f.map { case (maybeOwner, docs) => maybeOwner match {
      case Some(owner) => Ok(views.html.my.my_public(owner, docs, loggedInUser))
      case None => NotFoundPage
    }}
  }
  
  /** Takes a list of docIds and sorts them accourding to the given index property **/
  private def sortByIndexProperty(docIds: Seq[String], sortBy: String, sortOrder: SortOrder, offset: Int, pageSize: Int): Future[Seq[String]] = sortBy match {
    case "annotations" => annotations.sortDocsByAnnotationCount(docIds, sortOrder, offset, pageSize)
    case "last_modified_at" => contributions.sortDocsByLastModifiedAt(docIds, sortOrder, offset, pageSize)
    case "last_modified_by" => contributions.sortDocsByLastModifiedBy(docIds, sortOrder, offset, pageSize)
    case _ => Future.successful(docIds)
  }
  
  /** Takes a list of document IDs and, for each, fetches last edit and number of annotations from the index **/
  private def fetchIndexedProperties(docIds: Seq[String]): Future[Seq[(String, Option[Contribution], Long)]] = {
    val fLastEdits = Future.sequence(docIds.map(id => contributions.getLastContribution(id).map((id, _))))
    val fAnnotationsPerDoc = Future.sequence(docIds.map(id => annotations.countByDocId(id).map((id, _))))
      
    val f = for {
      lastEdits <- fLastEdits
      annotationsPerDoc <- fAnnotationsPerDoc
    } yield (lastEdits.toMap, annotationsPerDoc.toMap)   
    
    f.map { case (lastEdits, annotationsPerDoc) =>
      docIds.map { id =>
        val lastEdit = lastEdits.find(_._1 == id).flatMap(_._2)
        val annotations = annotationsPerDoc.find(_._1 == id).map(_._2).getOrElse(0l)
        (id, lastEdit, annotations)
      }
    }
  }
  
  private def renderMyDocuments(user: User, usedSpace: Long, offset: Int, sortBy: String, sortOrder: SortOrder, size: Option[Int])(implicit request: RequestHeader) = {
    val startTime = System.currentTimeMillis
    val fSharedCount = documents.countBySharedWith(user.username)
    val pageSize = size.getOrElse(DEFAULT_DOCUMENTS_PER_PAGE)

    val fMyDocs =
      if (isSortingByIndex(sortBy)) {
        documents.listAllIdsByOwner(user.username).flatMap { docIds =>
          val f = for {
            sortedIds <- sortByIndexProperty(docIds, sortBy, sortOrder, offset, pageSize)
            docs <- documents.findByIds(sortedIds)
          } yield (sortedIds, docs)
          
          f.map { case (sortedIds, docs) => 
            val sorted = sortedIds.map(id => docs.find(_.getId == id).get)
            Page(System.currentTimeMillis - startTime, docIds.size, offset, pageSize, sorted)
          }
        }
      } else {
        documents.findByOwner(user.username, offset, pageSize, Some(sortBy), Some(sortOrder))
      }
        
    val f = for {
      sharedCount <- fSharedCount
      myDocs <- fMyDocs
      indexedProps <- fetchIndexedProperties(myDocs.items.map(_.getId))
    } yield (sharedCount, myDocs.zip(indexedProps)
        .map({ case (doc, (_, lastEdit, annotations)) => (doc, lastEdit, annotations) }, System.currentTimeMillis - startTime))
    
    f.map { case (sharedCount, page) =>
      Ok(views.html.my.my_private(user, usedSpace, page, sharedCount, sortBy, sortOrder, size))
    }
  }

  private def renderSharedWithMe(user: User, usedSpace: Long, offset: Int, sortBy: String, sortOrder: SortOrder, size: Option[Int])(implicit request: RequestHeader) = {
    val startTime = System.currentTimeMillis    
    val fMyDocsCount = documents.countByOwner(user.username, false)
    val pageSize = size.getOrElse(DEFAULT_DOCUMENTS_PER_PAGE)
    
    val fDocsSharedWithMe =
      if (isSortingByIndex(sortBy)) {
        documents.listAllIdsSharedWith(user.username).flatMap { docIds =>           
          val f = for {
            sortedIds <- sortByIndexProperty(docIds, sortBy, sortOrder, offset, pageSize)
            docs <- documents.findByIdsWithSharingPolicy(sortedIds, user.username)
          } yield (sortedIds, docs)

          f.map { case (sortedIds, docs) => 
            val sorted = sortedIds.map(id => docs.find(_._1.getId == id).get)
            Page(System.currentTimeMillis - startTime, docIds.size, offset, pageSize, sorted) 
          }
        }
      } else {
        documents.findBySharedWith(user.username, offset, pageSize, Some(sortBy), Some(sortOrder))
      }
    
    val f = for {
      myDocsCount <- fMyDocsCount
      docsSharedWithMe <- fDocsSharedWithMe
      indexedProps <- fetchIndexedProperties(docsSharedWithMe.items.map(_._1.getId))
    } yield (myDocsCount, docsSharedWithMe.zip(indexedProps)
        .map({ case ((doc, sharingPolicy), (_, lastEdit, annotations)) => (doc, sharingPolicy, lastEdit, annotations) }, System.currentTimeMillis - startTime))

    f.map { case (myDocsCount, page) =>
      Ok(views.html.my.my_shared(user, usedSpace, myDocsCount, page, sortBy, sortOrder, size))
    }
  }
  
  /** A convenience '/my' route that redirects to the personal index **/
  def my = play.api.mvc.Action { Ok } /* StackAction { implicit request =>
    loggedIn match {
      case Some(userWithRoles) =>
        Redirect(routes.MyRecogitoController.index(userWithRoles.username.toLowerCase, None, None, None, None, None))

      case None =>
        // Not logged in - go to log in and then come back here
        Redirect(controllers.landing.routes.LoginLogoutController.showLoginForm(None))
          .withSession("access_uri" -> routes.MyRecogitoController.my.url)
    }
  } */

  def index(usernameInPath: String, tab: Option[String], page: Option[Int], sortBy: Option[String], order: Option[String], size: Option[Int]) = play.api.mvc.Action { Ok } /* AsyncStack { implicit request =>
    // If the user is logged in & the name in the path == username it's the profile owner
    val isProfileOwner = loggedIn match {
      case Some(userWithRoles) => userWithRoles.username.equalsIgnoreCase(usernameInPath)
      case None => false
    }
    
    val offset = (page.getOrElse(1) - 1) * size.getOrElse(DEFAULT_DOCUMENTS_PER_PAGE)

    if (isProfileOwner) {
      val normalizedSortOrder = order match {
        case None => 
          // No sort order specified: use default
          Some(SortOrder.ASC)
        case Some(str) =>
          // Sort order specified: use only if valid
          SortOrder.fromString(str)
      }
      
      (sortBy, normalizedSortOrder) match {
        
        case (Some(s), Some(o)) =>
          val user = loggedIn.get
          val usedSpace = uploads.getUsedDiskspaceKB(user.username)    
          tab match {
            case Some(t) if t.equals("shared") => renderSharedWithMe(user, usedSpace, offset, s, o, size)
            case _ => renderMyDocuments(user, usedSpace, offset, s, o, size)
          }
          
        case _ =>
          // DB natural order is pretty useless - we'll allow access to this URL 
          // without sort order, but will redirect to sorted by most recent edit
          Future.successful(Redirect(controllers.my.routes.MyRecogitoController.index(usernameInPath, tab, page, Some("last_modified_at"), Some("asc"), size)))
          
      }
    } else {
      renderPublicProfile(usernameInPath, loggedIn)
    }
  } */
  
}
