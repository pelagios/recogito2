package controllers.admin.gazetteers

import controllers.{ BaseAuthController, WebJarAssets }
import javax.inject.Inject
import models.document.DocumentService
import models.place.{ GazetteerUtils, PlaceService }
import models.user.UserService
import models.user.Roles._
import play.api.{ Configuration, Logger }
import scala.concurrent.{ ExecutionContext, Future }

class GazetteerAdminController @Inject() (
    val config: Configuration,
    val documents: DocumentService,
    val places: PlaceService,
    val users: UserService,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarAssets
  ) extends BaseAuthController(config, documents, users) {
  
  def index = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    places.listGazetteers().map { gazetteers => 
      Ok(views.html.admin.gazetteers.index(gazetteers))
    }
  }
  
  def importGazetteer = StackAction(AuthorityKey -> Admin) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("gazetteer-file")) match {
      case Some(formData) => {
        Future {
          scala.concurrent.blocking {
            Logger.info("Importing gazetteer from " + formData.filename)
            GazetteerUtils.importRDFStream(formData.ref.file, formData.filename, places)
          }
        }
        Redirect(routes.GazetteerAdminController.index)
      }
        
      case None => BadRequest
        
    }
  }
  
  def deleteGazetteer(name: String) = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    places.deleteByGazetteer(name).map { _ =>
      Status(200)
    }
  }

}