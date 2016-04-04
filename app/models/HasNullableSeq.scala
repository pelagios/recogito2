package models

trait HasNullableSeq {

  protected def fromOptSeq[T](o: Option[Seq[T]]) =
    o.getOrElse(Seq.empty[T])
  
  protected def toOptSeq[T](s: Seq[T]) =
    if (s.isEmpty) None else Some(s)
  
}