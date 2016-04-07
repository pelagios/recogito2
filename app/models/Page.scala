package models

/** A simple helper for wrapping paginated query results **/
case class Page[A](items: Seq[A], offset: Int, limit: Int, total: Long, query: Option[String] = None) {

  /** Helper to perform a map to the items in the page **/
  def map[B](f: (A) => B): Page[B] =
    Page(items.map(f), offset, limit, total, query)

}

object Page {

  /** Helper to create an empty page **/
  def empty[A] = Page(Seq.empty[A], 0, Int.MaxValue, 0)

}
