package models

import java.sql.Timestamp
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Tag

case class User(

  /** We're using the username as the unique key **/
  username: String,

  /** For verification and notifications **/
  email: String,

  /** Time when account was created **/
  memberSince: Timestamp,

  /** Login verification via salted password hash **/
  passwordHash: String,

  salt: String)

class Users(tag: Tag) extends Table[User](tag, "users") {

}
