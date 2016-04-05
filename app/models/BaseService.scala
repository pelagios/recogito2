package models

import org.jooq.Record
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.reflect.ClassTag
import storage.DB

import play.api.Logger

trait BaseService {
  
  private def isNotNull(record: Record) =
    (0 to record.size - 1).map(idx => {
      record.getValue(idx) != null
    }).exists(_ == true)

  protected def groupJoinResult[T <: Record, V <: Record](records: Seq[Record], t: Class[T], v: Class[V]) =
    records
      .map(r => (r.into(t), r.into(v)))
      .groupBy(_._1)
      .mapValues(_.map(_._2).filter(record => isNotNull(record)))
  
  /** Boilerplate code for conducting a cache lookup, followed by DB lookup if nothing in cache **/ 
  protected def cachedLookup[T: ClassTag](prefix: String, key: String, dbLookup: String => Future[Option[T]])(implicit db: DB, cache: CacheApi): Future[Option[T]] = {
    val maybeCachedValue = cache.get[T](prefix + "_" + key)    
    if (maybeCachedValue.isDefined) {
      Future.successful(maybeCachedValue)
    } else {
      dbLookup(key).map(maybeStoredValue => {
        if (maybeStoredValue.isDefined)
          cache.set(prefix + "_" + key, maybeStoredValue.get, 10 minutes)

        maybeStoredValue
      })
    }
  }

}
