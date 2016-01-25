package models

sealed trait Role
case object Admin extends Role
case object Normal extends Role