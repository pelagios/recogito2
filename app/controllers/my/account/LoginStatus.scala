package controllers.my.account

import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.user.User

case class LoginStatus(loggedIn: Boolean, user: Option[User])

object LoginStatus {

  val NOT_LOGGED_IN = LoginStatus(false, None)

  def as(user: User) = LoginStatus(true, Some(user))
 
  implicit val loginStatusWrites: Writes[LoginStatus] = (
    (JsPath \ "logged_in").write[Boolean] and
    (JsPath \ "username").writeNullable[String] and
    (JsPath \ "real_name").writeNullable[String]
  )(l => (
      l.loggedIn,
      l.user.map(_.username),
      l.user.flatMap(_.realName)
  ))

}