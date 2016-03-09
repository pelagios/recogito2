package models

import org.jooq.Record

trait BaseService {

  def groupJoinResult[T <: Record, V <: Record](records: Seq[Record], t: Class[T], v: Class[V]) = {
    records
      .map(r => (r.into(t), r.into(v)))
      .groupBy(_._1)
      .mapValues(_.map(_._2))
  }

}
