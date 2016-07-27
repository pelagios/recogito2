package controllers.document.settings.actions
  
import controllers.document.settings.SettingsController
import java.util.UUID
import models.document.PartOrdering
import models.user.Roles._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

trait MetadataActions { self: SettingsController =>
  
  implicit val orderingReads: Reads[PartOrdering] = (
    (JsPath \ "id").read[UUID] and
    (JsPath \ "sequence_no").read[Int]
  )(PartOrdering.apply _)
  
  /** Sets the part sort order **/
  def setSortOrder(docId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request => 
    jsonDocumentAdminAction[Seq[PartOrdering]](docId, loggedIn.user.getUsername, { case (document, ordering) =>
      documents.setFilepartSortOrder(docId, ordering).map(_ => Status(200))
    })
  }
  
}