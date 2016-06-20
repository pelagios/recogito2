package controllers.document.settings.actions
  
import controllers.BaseController
import controllers.document.settings.HasAdminAction
import models.document.{ DocumentService, PartOrdering }
import models.user.Roles._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

trait MetadataActions extends HasAdminAction { self: BaseController =>
  
  implicit val orderingReads: Reads[PartOrdering] = (
    (JsPath \ "id").read[Int] and
    (JsPath \ "sequence_no").read[Int]
  )(PartOrdering.apply _)
  
  /** Sets the part sort order **/
  def setSortOrder(docId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request => 
    jsonDocumentAdminAction[Seq[PartOrdering]](docId, loggedIn.user.getUsername, { case (document, ordering) =>
      DocumentService.setFilepartSortOrder(docId, ordering)(self.db).map(_ => Status(200))
    })
  }
  
}