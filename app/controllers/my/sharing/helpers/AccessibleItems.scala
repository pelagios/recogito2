package controllers.my.sharing.helpers

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import services.SharingLevel.Utils._
import services.folder.FolderService
import services.document.DocumentService
import services.generated.tables.records.{DocumentRecord, FolderRecord, SharingPolicyRecord}

case class AccessibleItems(
  folders: Seq[(FolderRecord, Option[SharingPolicyRecord])], 
  documents: Seq[(DocumentRecord, Option[SharingPolicyRecord])]
)

object AccessibleItems {

  object Utils {
      
    /** A helper function that takes a root folder ID and 
      * the current user, and returns the flattenend lists
      * of sub-folders and nested documents that are
      * visible to the given user.
      */
    def listAccessibleItemsRecursive(
      folderId: UUID,
      loggedInAs: String
    )(implicit 
        folderService: FolderService,
        documentService: DocumentService,
        ctx: ExecutionContext
    ): Future[AccessibleItems] = {

      val f = for {
        // All folders: root + nested subfolders
        folderIds <- folderService.getAllSubfoldersRecursive(folderId).map(sub => folderId +: sub.map(_._1))
        folders <- folderService.getFolders(folderIds, loggedInAs)
        documents <- folderService.listDocumentsInFolders(folderIds, loggedInAs)
      } yield (folders, documents)

      f.map { case (folders, documents) => 

        // Zip the documents list with info about each doc's parent folder
        val documentsWithParentFolder = documents.map { case (doc, policy, folderId) => 
          (doc, policy, folders.find(_._1.getId == folderId).get)
        }

        // Keep only documents where we have access on the parent folder
        val allowedDocuments = documentsWithParentFolder.filter { case (doc, policy, f) => 
          isFolderAdmin(loggedInAs, f._1, f._2)
        } map { case t => (t._1, t._2) }

        AccessibleItems(folders, allowedDocuments)
      }
    }

  }

}