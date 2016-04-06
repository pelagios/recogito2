package models.user

import models.generated.tables.records.{ UserRecord, UserRoleRecord }

case class UserWithRoles(user: UserRecord, roles: Seq[UserRoleRecord]) {
  
  def hasRole(role: Roles.Role): Boolean = roles.exists(_.getHasRole == role.toString)
  
}

object Roles { 
  
  sealed trait Role 
  
  case object Admin extends Role { override lazy val toString = "ADMIN" }

  case object Normal extends Role { override lazy val toString = "NORMAL" }

}

