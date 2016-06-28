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

case class AccountSettingsData(email: String, name: Option[String], bio: Option[String], website: Option[String])

class AccountSettingsController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController {

  val accountSettingsForm = Form(
    mapping(
      "email" -> email,
      "name" -> optional(text),
      "bio" -> optional(text),
      "website" -> optional(text)
    )(AccountSettingsData.apply)(AccountSettingsData.unapply)
  )

  def index() = StackAction(AuthorityKey -> Normal) { implicit request =>
    Ok(views.html.my.settings.account(accountSettingsForm))
  }

  def updateAccountSettings() = StackAction(AuthorityKey -> Normal) { implicit request =>
    Ok("TODO")
  }

}
