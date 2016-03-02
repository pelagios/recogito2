package models

import models.generated.Tables._
import org.jooq.Record
import storage.DB
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.reflect.ClassTag
import play.api.Logger

object DocumentService {
  
  /** TODO move this into a general "Service" superclass or trait **/
  def groupJoin2Result[T <: Record, V <: Record](records: Seq[Record], t: Class[T], v: Class[V]) = {
    records
      .map(r => (r.into(t), r.into(v)))
      .groupBy(_._1)
      .mapValues(_.map(_._2))
  }

  def findById(id: Int)(implicit db: DB) = db.query { sql =>
    Option(sql.selectFrom(DOCUMENT).where(DOCUMENT.ID.equal(id)).fetchOne())
  }
  
  def findByUser(username: String, offset: Int = 0, limit: Int = 20)(implicit db: DB) = db.query { sql =>
    sql.selectFrom(DOCUMENT)
       .where(DOCUMENT.OWNER.equal(username))
       .limit(limit)
       .offset(offset)
       .fetchArray().toSeq
  }
  
  def findByIdWithFileparts(id: Int)(implicit db: DB) = db.query { sql =>
    val records = 
      sql.selectFrom(DOCUMENT
        .join(DOCUMENT_FILEPART)
        .on(DOCUMENT.ID.equal(DOCUMENT_FILEPART.DOCUMENT_ID)))
      .where(DOCUMENT.ID.equal(id))
      .fetchArray()
      
    // Convert to (DocumentRecord, Seq[DocumentFilepartRecord) tuple
    val grouped = groupJoin2Result(records, classOf[DocumentRecord], classOf[DocumentFilepartRecord])
    if (grouped.size > 1)
      throw new RuntimeException("Got " + grouped.size + " DocumentRecords with the same ID: " + grouped.keys.map(_.getId).mkString(", "))
    
    grouped.headOption
  }

}
