package controllers.my.directory.create.helpers

import scala.concurrent.{ExecutionContext, Future}
import services.SharingLevel
import services.document.DocumentService
import services.folder.FolderService
import services.generated.tables.records.{DocumentRecord, FolderRecord, SharingPolicyRecord}

trait InheritCollaboratorsHelper { 

  def inheritCollaborators(
    folder: FolderRecord,
    loggedInAs: String
  )(implicit 
      folderService: FolderService,
      ctx: ExecutionContext
  ): Future[Boolean] = {
    
    def applyPolicies(policies: Seq[SharingPolicyRecord]): Future[Boolean] = 
      Future.sequence {
        policies.map { p =>
          folderService.addCollaborator(
            folder.getId, 
            p.getSharedBy, 
            p.getSharedWith, 
            SharingLevel.withName(p.getAccessLevel).get
          )
        }
      } map { !_.exists(_ == false) }

    Option(folder.getParent) match {
      case Some(parentId) => 
        for {
          parentCollaborators <- folderService.getCollaborators(parentId)
          success <- applyPolicies(parentCollaborators)
        } yield (success)

      case None =>
        Future.successful(true)
    } 
  }  

  def inheritCollaborators(
    document: DocumentRecord
  )(implicit 
      documentService: DocumentService,
      folderService: FolderService,
      ctx: ExecutionContext
  ): Future[Boolean] = {

    def applyPolicies(policies: Seq[SharingPolicyRecord]): Future[Boolean] =
      Future.sequence {
        policies.map { p => 
          documentService.addDocumentCollaborator(
            document.getId,
            p.getSharedBy,
            p.getSharedWith,
            SharingLevel.withName(p.getAccessLevel).get
          )
        }
      } map { !_.exists(_ == false) }

    folderService.getContainingFolder(document.getId).flatMap { _ match {
      case Some(folder) => 
        for {
          folderCollaborators <- folderService.getCollaborators(folder.getId)
          success <- applyPolicies(folderCollaborators)
        } yield (success)

      case None => 
        // No parent folder
        Future.successful(true)
    }}
  }

}