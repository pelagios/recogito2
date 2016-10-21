package controllers.my.settings

import controllers.{ HasUserService, HasConfig, Security }
import java.io.File
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.user.Roles._
import models.user.UserService
import play.api.{ Configuration, Logger }
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.Controller
import scala.concurrent.{ ExecutionContext, Future }

class RestoreController @Inject() (
    val config: Configuration,
    val users: UserService,
    val messagesApi: MessagesApi,
    implicit val ctx: ExecutionContext
) extends Controller with AuthElement with HasUserService with HasConfig with Security with I18nSupport {
  
  private def restoreFromZip(zipfile: File, currentUser: String): Future[Unit] = {
    Future { }
  }

  def index() = StackAction(AuthorityKey -> Normal) { implicit request =>
    Ok(views.html.my.settings.restore(loggedIn.user))
  }

  def restore() = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    request.body.asMultipartFormData.map { tempfile =>
      tempfile.file("backup") match {
       
        case Some(filepart) =>
          restoreFromZip(filepart.ref.file, loggedIn.user.getUsername)
            .map { _ => Redirect(routes.RestoreController.index) }
            .recover { case t: Throwable =>
              t.printStackTrace()
              InternalServerError("There was an error restoring your document")
            }
          
        case None =>
          Logger.warn("Personal document restore POST without file attached")
          Future.successful(BadRequest)        
      }
    }.getOrElse {
      Logger.warn("Personal document restore POST without form data")
      Future.successful(BadRequest)
    }
  }

}
