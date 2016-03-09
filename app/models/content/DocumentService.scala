package models.content

import models.BaseService
import models.generated.Tables._
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.jooq.Record
import scala.reflect.ClassTag
import storage.DB

object DocumentService extends BaseService {

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
    val grouped = groupJoinResult(records, classOf[DocumentRecord], classOf[DocumentFilepartRecord])
    if (grouped.size > 1)
      throw new RuntimeException("Got " + grouped.size + " DocumentRecords with the same ID: " + grouped.keys.map(_.getId).mkString(", "))

    grouped.headOption
  }
  
  def findPartByDocAndSeqNo(docId: Int, seqNo: Int)(implicit db: DB) = db.query { sql =>
    Option(sql.selectFrom(DOCUMENT_FILEPART)
              .where(DOCUMENT_FILEPART.DOCUMENT_ID.equal(docId))
              .and(DOCUMENT_FILEPART.SEQUENCE_NO.equal(seqNo))
              .fetchOne())
  }

}
