package controllers.my.settings

import controllers.{ HasUserService, HasConfig, Security, WebJarAssets }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.{ AuthElement, Logout }
import models.annotation.AnnotationService
import models.contribution.ContributionService
import models.user.Roles._
import models.user.UserService
import models.upload.UploadService
import models.document.DocumentService
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.Controller
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._

case class AccountSettingsData(email: String, name: Option[String], bio: Option[String], website: Option[String])

class AccountSettingsController @Inject() (
    val config: Configuration,
    val users: UserService,
    val annotations: AnnotationService,
    val contributions: ContributionService,
    val documents: DocumentService,
    val uploads: UploadService,
    val messagesApi: MessagesApi,
    implicit val webjars: WebJarAssets,
    implicit val ctx: ExecutionContext
  ) extends Controller with AuthElement with HasUserService with HasConfig with Security with Logout with I18nSupport {

  val accountSettingsForm = Form(
    mapping(
      "email" -> email,
      "name" -> optional(text(maxLength=80)),
      "bio" -> optional(text(maxLength=256)),
      "website" -> optional(text(maxLength=256))
    )(AccountSettingsData.apply)(AccountSettingsData.unapply)
  )

  def index() = StackAction(AuthorityKey -> Normal) { implicit request =>
    val form = accountSettingsForm.fill(AccountSettingsData(
      users.decryptEmail(loggedIn.user.getEmail),
      Option(loggedIn.user.getRealName),
      Option(loggedIn.user.getBio),
      Option(loggedIn.user.getWebsite)))
    
    Ok(views.html.my.settings.account(form, loggedIn.user))
  }

  def updateAccountSettings() = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    accountSettingsForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.my.settings.account(formWithErrors, loggedIn.user))),

      f =>
        users.updateUserSettings(loggedIn.user.getUsername, f.email, f.name, f.bio, f.website)
          .map { success =>
            if (success)
              Redirect(routes.AccountSettingsController.index).flashing("success" -> "Your settings have been saved.")
            else 
              Redirect(routes.AccountSettingsController.index).flashing("error" -> "There was an error while saving your settings.")
          }.recover { case t:Throwable => {
            t.printStackTrace()
            Redirect(routes.AccountSettingsController.index).flashing("error" -> "There was an error while saving your settings.")
          }}
    )
  }
  
  def deleteAccount() = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    
    def deleteFromIndex(documentIds: Seq[String]) = {  
      def deleteOneDocument(docId: String): Future[Unit] = {
        // Annotations, geo-tags and version history
        val deleteAnnotations = annotations.deleteByDocId(docId)
          
        // Contributions
        val deleteContributions = contributions.deleteHistory(docId) 
          
        for {
          _ <- deleteAnnotations
          _ <- deleteContributions
        } yield ()
      }
      
      Future {
        scala.concurrent.blocking {
          documentIds.foreach(id => Await.result(deleteOneDocument(id), 10.second))
        }
      }
    }
    
    val username = loggedIn.user.getUsername
    
    // Fetch IDs of all documents owned by this user
    val fOwnedDocumentIds = documents.listAllIdsByOwner(username)
        
    // Delete pending upload & upload_filepart records
    val fDeletePendingUpload = uploads.deletePendingUpload(username)
    
    // Delete sharing policies shared by and with this user
    val fDeleteSharingPolicies = documents.deleteAffectedPolicies(username)
        
    val f = for {
      ids <- fOwnedDocumentIds
      _ <- fDeletePendingUpload
      _ <- fDeleteSharingPolicies
      
      // Delete owned documents, document_fileparts & sharing policies linked to them
      _ <- documents.deleteByOwner(username) 
      
      // Delete annotations, history, geotags & contributions
      _ <- deleteFromIndex(ids)

      // User & roles
      _ <- users.deleteByUsername(username)
    } yield ()
    
    f.map { _ =>
      gotoLogoutSucceeded
      Ok
    }
  }

}
