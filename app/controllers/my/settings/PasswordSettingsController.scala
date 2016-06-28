package controllers.my.settings

import controllers.BaseController
import javax.inject.Inject
import models.user.Roles._
import play.api.Play.current
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages.Implicits._
import storage.DB

case class PasswordSettingsData(currentPassword: String, newPassword: String, verifyPassword: String)

class PasswordSettingsController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController {

  val passwordSettingsForm = Form(
    mapping(
      "current" -> nonEmptyText,
      "new" -> nonEmptyText,
      "verify" -> nonEmptyText
    )(PasswordSettingsData.apply)(PasswordSettingsData.unapply)
  )

  def index() = StackAction(AuthorityKey -> Normal) { implicit request =>
    Ok(views.html.my.settings.password(passwordSettingsForm))
  }

  def updatePassword() = StackAction(AuthorityKey -> Normal) { implicit request =>
    Ok("TODO")
  }

}
