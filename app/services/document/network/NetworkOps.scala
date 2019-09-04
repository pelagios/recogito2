package services.document.network

import scala.concurrent.{ExecutionContext, Future}
import services.document.DocumentService

trait NetworkOps { self: DocumentService => 

  /** Traverses up the clone hierarchy and gets the top-most ancestor **/
  private def getNetworkRoot(docId: String): Future[Option[AncestryTreeNode]] = db.query { sql => 
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

    Option(sql.resultQuery(query, docId).fetchOne).flatMap { result => 
      val id = result.getValue("id", classOf[String])
      val owner = result.getValue("owner", classOf[String])

      // Note that the result always includes the document itself, too. I.e. if
      // there is no ancestor, the result array will have length one, with just 
      // the doc -> discard this result
      if (id != docId)
        Some(AncestryTreeNode(id, owner))
      else 
        None
    }
  }

  private def getDescendants(docId: String): Future[Seq[AncestryTreeNode]] = db.query { sql => 
    val query =
      """
      WITH RECURSIVE descendants AS (
        SELECT
          document.id,
          document.owner,
          document.cloned_from,
          1 as level
        FROM document
        WHERE cloned_from IS NOT NULL
      UNION ALL
        SELECT doc.id, doc.owner, doc.cloned_from, parent.level + 1
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
      AncestryTreeNode(id, owner, Option(clonedFrom))
    }
  }

  /** Returns the clone network tree for the given document **/
  def getNetwork(docId: String)(implicit ctx: ExecutionContext): Future[Option[AncestryTree]] = {
    val f = for {
      maybeRoot <- getNetworkRoot(docId)
      descendants <- maybeRoot.map(rootNode => getDescendants(rootNode.id))
                       .getOrElse(Future.successful(Seq.empty[AncestryTreeNode]))
    } yield (maybeRoot, descendants)

    f.map { case (maybeRoot, descendants) => 
      maybeRoot.map(rootNode => AncestryTree(rootNode, descendants))
    }
  }

}