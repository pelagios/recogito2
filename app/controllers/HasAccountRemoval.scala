package controllers

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import services.announcement.AnnouncementService
import services.annotation.AnnotationService
import services.contribution.ContributionService
import services.document.DocumentService
import services.upload.UploadService
import services.user.UserService

trait HasAccountRemoval {

  private def deleteDocumentsAndAnnotations(documentIds: Seq[String])(implicit 
    annotations: AnnotationService,
    context: ExecutionContext,
    contributions: ContributionService
  ) = {
    
    def deleteOneDocument(docId: String): Future[Unit] = {
      // Annotations, geo-tags and version history
      val deleteAnnotations = annotations.deleteByDocId(docId)
        
      // Contributions
      val deleteContributions = contributions.deleteHistory(docId) 
        
      for {
        _ <- deleteAnnotations
        _ <- deleteContributions
      } yield ()
    }
    
    Future {
      scala.concurrent.blocking {
        documentIds.foreach(id => Await.result(deleteOneDocument(id), 10.second))
      }
    }
  }
  
  def deleteUserAccount(username: String)(implicit
    announcements: AnnouncementService,
    annotations: AnnotationService,
    context: ExecutionContext,
    contributions: ContributionService,
    documents: DocumentService,
    uploads: UploadService,
    users: UserService
  ) = {   
    
    // Fetch IDs of all documents owned by this user
    val fOwnedDocumentIds = documents.listAllIdsByOwner(username)
        
    // Delete pending upload & upload_filepart records
    val fDeletePendingUpload = uploads.deletePendingUpload(username)
    
    // Delete sharing policies shared by and with this user
    val fDeleteSharingPolicies = documents.deleteAffectedPolicies(username)
    
    // Delete pending/archived announcements for this user, if any
    val fDeleteAnnouncements = announcements.deleteForUser(username)
        
    for {
      ids <- fOwnedDocumentIds
      _ <- fDeletePendingUpload
      _ <- fDeleteSharingPolicies
      _ <- fDeleteAnnouncements
      
      // Delete owned documents, document_fileparts & sharing policies linked to them
      _ <- documents.deleteByOwner(username) 
      
      // Delete annotations, history, geotags & contributions
      _ <- deleteDocumentsAndAnnotations(ids)

      // User & roles
      _ <- users.deleteByUsername(username)
    } yield ()  
  }

}