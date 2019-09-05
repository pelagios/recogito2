package services.document.network

import java.sql.Timestamp
import scala.concurrent.{ExecutionContext, Future}
import services.document.DocumentService

trait NetworkOps { self: DocumentService => 

  /** Traverses up the clone hierarchy and gets the top-most ancestor. 
    *
    * If there is no network, the document itself is the "root".
    */
  private def getNetworkRoot(docId: String): Future[Option[TreeRecord]] = db.query { sql => 
    val query = 
      """
      WITH RECURSIVE path AS (
        SELECT 
          document.id,
          document.owner,
          document.cloned_from,
          ARRAY[id] AS path_ids,
          ARRAY[owner] AS path_owners
        FROM document
        WHERE cloned_from IS NULL
      UNION ALL
        SELECT d.id, d.owner, d.cloned_from,
          p.path_ids || d.id,
          p.path_owners || d.owner
        FROM document d
        JOIN path p
          ON d.cloned_from = p.id
      )
      SELECT
        path_ids[1] AS id, path_owners[1] AS owner
      FROM path WHERE id = ? ;
      """

    Option(sql.resultQuery(query, docId).fetchOne).map { result => 
      val id = result.getValue("id", classOf[String])
      val owner = result.getValue("owner", classOf[String])
      TreeRecord(id, owner)
    }
  }

  private def getDescendants(docId: String): Future[Seq[TreeRecord]] = db.query { sql => 
    val query =
      """
      WITH RECURSIVE descendants AS (
        SELECT
          document.id,
          document.owner,
          document.cloned_from,
          document.uploaded_at,
          1 as level
        FROM document
        WHERE cloned_from IS NOT NULL
      UNION ALL
        SELECT doc.id, doc.owner, doc.cloned_from, doc.uploaded_at, parent.level + 1
        FROM document doc
        JOIN descendants parent
          ON doc.cloned_from = parent.id
      ) 
      SELECT * FROM descendants WHERE cloned_from = ?
      ORDER BY id, level, cloned_from ;
      """

    sql.resultQuery(query, docId).fetchArray.map { row => 
      val id = row.getValue("id", classOf[String])
      val owner = row.getValue("owner", classOf[String])
      val clonedFrom = row.getValue("cloned_from", classOf[String])
      val uploadedAt = row.getValue("uploaded_at", classOf[Timestamp])
      TreeRecord(id, owner, Option(clonedFrom), Option(uploadedAt))
    }
  }

  /** Returns the clone network tree for the given document **/
  def getNetwork(docId: String)(implicit ctx: ExecutionContext): Future[Option[AncestryTree]] = {
    val f = for {
      maybeRoot <- getNetworkRoot(docId)
      descendants <- maybeRoot.map(rootNode => getDescendants(rootNode.id))
                       .getOrElse(Future.successful(Seq.empty[TreeRecord]))
    } yield (maybeRoot, descendants)

    f.map { case (maybeRoot, descendants) => 
      maybeRoot.map(rootNode => AncestryTree(rootNode, descendants))
    }
  }

}