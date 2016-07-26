package controllers.admin.gazetteers

import controllers.{ BaseAuthController, ControllerContext }
import javax.inject.Inject
import models.place.{ GazetteerUtils, PlaceService }
import models.user.Roles._
import play.api.Logger
import scala.concurrent.Future

class GazetteerAdminController @Inject() (implicit val ctx: ControllerContext) extends BaseAuthController {
  
  def index = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    PlaceService.listGazetteers().map { gazetteers => 
      Ok(views.html.admin.gazetteers.index(gazetteers))
    }
  }
  
  def importGazetteer = StackAction(AuthorityKey -> Admin) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("gazetteer-file")) match {
      case Some(formData) => {
        Future {
          scala.concurrent.blocking {
            Logger.info("Importing gazetteer from " + formData.filename)
            GazetteerUtils.importRDFStream(formData.ref.file, formData.filename)
          }
        }
        Redirect(routes.GazetteerAdminController.index)
      }
        
      case None => BadRequest
        
    }
  }
  
  def deleteGazetteer(name: String) = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    PlaceService.deleteByGazetteer(name).map { _ =>
      Status(200)
    }
  }

}