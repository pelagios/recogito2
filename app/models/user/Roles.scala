package models.user

object Roles { 
  
  sealed trait Role 
  
  case object Admin extends Role { override lazy val toString = "ADMIN" }

  case object Normal extends Role { override lazy val toString = "NORMAL" }

}