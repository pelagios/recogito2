package services.document

import java.util.UUID
import javax.inject.{Inject, Singleton}
import services.BaseService
import services.generated.Tables.DOCUMENT
import scala.concurrent.ExecutionContext
import storage.db.DB
import storage.uploads.Uploads

@Singleton
class DocumentService @Inject() (
  implicit val uploads: Uploads, 
  implicit val db: DB
) extends BaseService 
  with create.CreateOps
  with read.DocumentReadOps
  with read.FilepartReadOps
  with read.ReadFromFolderOps
  with read.CollaboratorReadOps
  with read.SharedWithMeReadOps
  with read.AccessibleDocumentOps
  with search.SearchOps
  with network.NetworkOps
  with update.DocumentUpdateOps
  with update.CollaboratorUpdateOps
  with delete.DeleteOps {

  private val SORTABLE_FIELDS = 
    DOCUMENT.fields.toSeq.map(_.getName)

  /** Just make sure people don't inject any weird shit into the sort field **/
  protected def sanitizeField(name: String): Option[String] = {
    SORTABLE_FIELDS.find(_ == name)
  }

  /** Or into doc ID queries somehow **/
  protected def sanitizeDocId(id: String): Option[String] = {
    if (id.length == DocumentIdFactory.ID_LENGTH && // Ids have fixed length
        id.matches("[a-z0-9]+")) { // Ids are all lowercase & alphanumeric
      Some(id)
    } else {
      None
    }
  }

  /** Shorthand **/
  def listIds(folder: Option[UUID], loggedInAs: String)(implicit ctx: ExecutionContext) =
    folder match {
      case Some(folderId) => 
        listDocumentsInFolder(folderId, loggedInAs)
          .map { _.map { case (doc, _) => 
            doc.getId
          }}

      case None => 
        listRootIdsByOwner(loggedInAs)
    }
      
}

