package services.folder

import javax.inject.{Inject, Singleton}
import services.BaseService
import storage.db.DB

/** The folder service class is just a container.
  * 
  * Actual service method implementations are in the respective 'Ops' 
  * traits - see below.
  */
@Singleton
class FolderService @Inject() (implicit val db: DB) extends BaseService
  with create.CreateOps
  with read.FolderReadOps
  with read.FolderListOps
  with read.BreadcrumbReadOps
  with update.UpdateOps
  with delete.DeleteOps 
    