package controllers.my

import controllers.{ BaseController, Security, WebJarAssets }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.OptionalAuthElement
import models.{ Page, SortOrder }
import models.annotation.AnnotationService
import models.contribution.{ Contribution, ContributionService }
import models.user.UserService
import models.document.DocumentService
import models.generated.tables.records.{ DocumentRecord, UserRecord }
import play.api.Configuration
import play.api.mvc.RequestHeader
import scala.concurrent.{ ExecutionContext, Future }

class MyRecogitoController @Inject() (
    val annotations: AnnotationService,
    val contributions: ContributionService,
    val documents: DocumentService,
    val users: UserService,
    val config: Configuration,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarAssets
  ) extends BaseController(config, users) with OptionalAuthElement {

  private val DOCUMENTS_PER_PAGE = 10
  
  private val INDEX_SORT_PROPERTIES = Seq("last_modified_at", "last_modified_by", "annotations")
  
  private def isSortingByIndex(sortBy: Option[String]) =
    sortBy.map(fieldname => INDEX_SORT_PROPERTIES.contains(fieldname.toLowerCase)).getOrElse(false)

  private def renderPublicProfile(username: String, loggedInUser: Option[UserRecord])(implicit request: RequestHeader) = {
    val f = for {
      userWithRoles   <- users.findByUsernameIgnoreCase(username)
      publicDocuments <- if (userWithRoles.isDefined)
                           documents.findByOwner(userWithRoles.get.user.getUsername, true)
                         else
                           Future.successful(Page.empty[DocumentRecord])
    } yield (userWithRoles, publicDocuments)

    f.map { case (userWithRoles, publicDocuments) => userWithRoles match {
      case Some(u) => Ok(views.html.my.my_public(u.user, publicDocuments, loggedInUser))
      case None => NotFoundPage
    }}
  }
  
  /** Takes a list of docIds and sorts them accourding to the given index property **/
  private def sortByIndexProperty(docIds: Seq[String], sortBy: String, sortOrder: SortOrder, offset: Int): Future[Seq[String]] = sortBy match {
    case "annotations" => annotations.sortDocsByAnnotationCount(docIds, sortOrder, offset, DOCUMENTS_PER_PAGE)
    case "last_modified_at" => contributions.sortDocsByLastModifiedAt(docIds, sortOrder, offset, DOCUMENTS_PER_PAGE)
    case "last_modified_by" => contributions.sortDocsByLastModifiedBy(docIds, sortOrder, offset, DOCUMENTS_PER_PAGE)
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
  
  private def renderMyDocuments(user: UserRecord, usedSpace: Long, offset: Int, sortBy: Option[String], sortOrder: Option[SortOrder])(implicit request: RequestHeader) = {
    val startTime = System.currentTimeMillis
    val fSharedCount = documents.countBySharedWith(user.getUsername)

    val fMyDocs =
      if (isSortingByIndex(sortBy)) {
        documents.listAllIdsByOwner(user.getUsername).flatMap { docIds =>
          val f = for {
            sortedIds <- sortByIndexProperty(docIds, sortBy.get, sortOrder.getOrElse(SortOrder.ASC), offset)
            docs <- documents.findByIds(sortedIds)
          } yield (sortedIds, docs)
          
          f.map { case (sortedIds, docs) => 
            val sorted = sortedIds.map(id => docs.find(_.getId == id).get)
            Page(System.currentTimeMillis - startTime, docIds.size, offset, DOCUMENTS_PER_PAGE, sorted)
          }
        }
      } else {
        documents.findByOwner(user.getUsername, false, offset, DOCUMENTS_PER_PAGE, sortBy, sortOrder)
      }
        
    val f = for {
      sharedCount <- fSharedCount
      myDocs <- fMyDocs
      indexedProps <- fetchIndexedProperties(myDocs.items.map(_.getId))
    } yield (sharedCount, myDocs.zip(indexedProps)
        .map({ case (doc, (_, lastEdit, annotations)) => (doc, lastEdit, annotations) }, System.currentTimeMillis - startTime))
    
    f.map { case (sharedCount, page) =>
      Ok(views.html.my.my_private(user, usedSpace, page, sharedCount, sortBy, sortOrder))
    }
  }

  private def renderSharedWithMe(user: UserRecord, usedSpace: Long, offset: Int, sortBy: Option[String], sortOrder: Option[SortOrder])(implicit request: RequestHeader) = {
    val startTime = System.currentTimeMillis    
    val fMyDocsCount = documents.countByOwner(user.getUsername, false)
    
    val fDocsSharedWithMe =
      if (isSortingByIndex(sortBy)) {
        documents.listAllIdsSharedWith(user.getUsername).flatMap { docIds =>           
          val f = for {
            sortedIds <- sortByIndexProperty(docIds, sortBy.get, sortOrder.getOrElse(SortOrder.ASC), offset)
            docs <- documents.findByIdsWithSharingPolicy(sortedIds, user.getUsername)
          } yield (sortedIds, docs)

          f.map { case (sortedIds, docs) => 
            val sorted = sortedIds.map(id => docs.find(_._1.getId == id).get)
            Page(System.currentTimeMillis - startTime, docIds.size, offset, DOCUMENTS_PER_PAGE, sorted) 
          }
        }
      } else {
        documents.findBySharedWith(user.getUsername, offset, DOCUMENTS_PER_PAGE, sortBy, sortOrder)
      }
    
    val f = for {
      myDocsCount <- fMyDocsCount
      docsSharedWithMe <- fDocsSharedWithMe
      indexedProps <- fetchIndexedProperties(docsSharedWithMe.items.map(_._1.getId))
    } yield (myDocsCount, docsSharedWithMe.zip(indexedProps)
        .map({ case ((doc, sharingPolicy), (_, lastEdit, annotations)) => (doc, sharingPolicy, lastEdit, annotations) }, System.currentTimeMillis - startTime))

    f.map { case (myDocsCount, page) =>
      Ok(views.html.my.my_shared(user, usedSpace, myDocsCount, page, sortBy, sortOrder))
    }
  }
  
  /** A convenience '/my' route that redirects to the personal index **/
  def my = StackAction { implicit request =>
    loggedIn match {
      case Some(userWithRoles) =>
        Redirect(routes.MyRecogitoController.index(userWithRoles.user.getUsername.toLowerCase, None, None, None, None))

      case None =>
        // Not logged in - go to log in and then come back here
        Redirect(controllers.landing.routes.LoginLogoutController.showLoginForm(None))
          .withSession("access_uri" -> routes.MyRecogitoController.my.url)
    }
  }

  def index(usernameInPath: String, tab: Option[String], page: Option[Int], sortBy: Option[String], order: Option[String]) = AsyncStack { implicit request =>
    // If the user is logged in & the name in the path == username it's the profile owner
    val isProfileOwner = loggedIn match {
      case Some(userWithRoles) => userWithRoles.user.getUsername.equalsIgnoreCase(usernameInPath)
      case None => false
    }
    
    val offset = (page.getOrElse(1) - 1) * DOCUMENTS_PER_PAGE
    
    val sortOrder = order.flatMap(SortOrder.fromString(_))

    if (isProfileOwner) {
      val user = loggedIn.get.user
      val usedSpace = users.getUsedDiskspaceKB(user.getUsername)

      tab match {
        case Some(t) if t.equals("shared") => renderSharedWithMe(user, usedSpace, offset, sortBy, sortOrder)
        case _ => renderMyDocuments(user, usedSpace, offset, sortBy, sortOrder)
      }
    } else {
      renderPublicProfile(usernameInPath, loggedIn.map(_.user))
    }
  }
  
}
