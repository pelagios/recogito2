package models

import java.sql.Timestamp

case class User(

  /** We're using the username as the unique key **/
  username: String,

  /** For verification and notifications **/
  email: String,

  /** Time when account was created **/
  memberSince: Timestamp)

  /** Login verification via salted password hash **
  passwordHash: String,

  salt: String)*/

/*
  object User {

    def listAll: List[User] = DB.withConnection { conn =>
      for (r: DataRecord <- conn
        selectFrom DATA
        fetch
      ) yield r

  }
  */
