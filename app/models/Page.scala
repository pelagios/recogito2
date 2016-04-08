package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

/** A simple helper for wrapping paginated query results **/
case class Page[A](total: Long, offset: Int, limit: Long, items: Seq[A]) {

  /** Helper to perform a map to the items in the page **/
  def map[B](f: (A) => B): Page[B] =
    Page(total, offset, limit, items.map(f))

}

object Page {

  /** Helper to create an empty page **/
  def empty[A] = Page(0, 0, Int.MaxValue, Seq.empty[A])
  
  /** JSON serialization **/
  implicit def pageWrites[A](implicit fmt: Writes[A]): Writes[Page[A]] = (
    (JsPath \ "total").write[Long] and
    (JsPath \ "offset").write[Int] and
    (JsPath \ "limit").write[Long] and
    (JsPath \ "items").write[Seq[A]]
  )(unlift(Page.unapply[A]))

}