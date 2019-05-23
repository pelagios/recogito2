package services.document.search

import org.joda.time.DateTime

case class SearchArgs(
  query: Option[String],
  searchIn: Scope,
  contentType: Option[DocumentType],
  owner: Option[String], 
  maxAge: Option[DateTime])

sealed trait Scope 

object Scope {

  case object ALL     extends Scope
  case object MY      extends Scope
  case object SHARED  extends Scope

  def withName(name: String): Option[Scope] =
    Seq(ALL, MY, SHARED).find(_.toString == name.toUpperCase)  

}

sealed trait DocumentType

object DocumentType {

  case object TEXT  extends DocumentType
  case object IMAGE extends DocumentType
  case object TABLE extends DocumentType

  def withName(name: String): Option[DocumentType] =
    Seq(TEXT, IMAGE, TABLE).find(_.toString == name.toUpperCase)

}