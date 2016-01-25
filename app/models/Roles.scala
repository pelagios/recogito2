package models

object Roles {
  
  sealed trait Role 
  
  case object Admin extends Role 

  case object Normal extends Role

}