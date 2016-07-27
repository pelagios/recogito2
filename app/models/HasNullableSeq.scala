package models

/** Convenience helper for serializing fields of type Seq[A] to JSON.
  *
  * In case the Seq is empty, the JSON can omit the field instead of requiring
  * a field with an empty array.
  */
trait HasNullableSeq {

  protected def fromOptSeq[T](o: Option[Seq[T]]) =
    o.getOrElse(Seq.empty[T])

  protected def toOptSeq[T](s: Seq[T]) =
    if (s.isEmpty) None else Some(s)

}
