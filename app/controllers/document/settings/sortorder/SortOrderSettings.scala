package controllers.document.settings.sortorder
  
import controllers.BaseController
import models.document._
import models.user.Roles._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

trait SortOrderSettings { self: BaseController =>
  
  implicit val orderingReads: Reads[PartOrdering] = (
    (JsPath \ "id").read[Int] and
    (JsPath \ "sequence_no").read[Int]
  )(PartOrdering.apply _)
  
  def setSortOrder(docId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    request.body.asJson match {
      case Some(json) => Json.fromJson[Seq[PartOrdering]](json) match {
        case s: JsSuccess[Seq[PartOrdering]] =>
          DocumentService.findById(docId, Some(loggedIn.user.getUsername))(self.db).flatMap(_ match {
            case Some((document, accesslevel)) if accesslevel.canWrite =>
              DocumentService.setFilepartSortOrder(docId, s.get)(self.db).map(_ => Status(200))
              
            case Some(_) =>
              Future.successful(Forbidden)
              
            case None =>
              Future.successful(NotFound)
          })
        
        case e: JsError => 
          Future.successful(BadRequest)
      }
        
      case None =>
        Future.successful(BadRequest)
    }    
  }
  
}