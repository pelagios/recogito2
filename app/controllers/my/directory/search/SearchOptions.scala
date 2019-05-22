package controllers.my.directory.search

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.{ContentType, HasDate}

case class SearchOptions(
  query: Option[String],
  searchIn: Scope.Scope,
  contentType: Option[ContentType],
  owner: Option[String], 
  maxAge: Option[DateTime])

object SearchOptions extends HasDate {

  implicit val searchOptionsReads: Reads[SearchOptions] = (
    (JsPath \ "q").readNullable[String] and
    (JsPath \ "in").readNullable[String]
      .map(_.flatMap(Scope.withName).getOrElse(Scope.ALL_OF_RECOGITO)) and
    (JsPath \ "doc_type").readNullable[String]
      .map(_.flatMap(ContentType.withName)) and
    (JsPath \ "owner").readNullable[String] and
    (JsPath \ "max_age").readNullable[DateTime]
  )(SearchOptions.apply _) 

}

object Scope {
  
  sealed trait Scope 
  case object ALL_OF_RECOGITO extends Scope
  case object MY_DOCUMENTS    extends Scope
  case object SHARED_WITH_ME  extends Scope

  def withName(name: String): Option[Scope] =
    Seq(ALL_OF_RECOGITO, MY_DOCUMENTS, SHARED_WITH_ME).find(_.toString == name)  

}