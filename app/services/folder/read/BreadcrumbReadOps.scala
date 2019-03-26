package services.folder.read

import java.util.UUID
import services.folder.{Breadcrumb, FolderService}

trait BreadcrumbReadOps { self: FolderService => 

  /** Gets the 'real' breadcrumb trail for the given folder.
    * 
    * The breadcrumb trail includes the current folder (with the specified
    * id) and all parent folders in the hierarchy, top to bottom. I.e. the 
    * first item in the list is the top-most ancestor.
    */
  def getBreadcrumbs(id: UUID) = db.query { sql => 
    // Capitulation. Gave up re-modelling this query in JOOQ, sorry.
    val query = 
      """
      WITH RECURSIVE path AS (
        SELECT id, owner, title, parent, 
          ARRAY[id] AS path_ids,
          ARRAY[title] AS path_titles
        FROM folder
        WHERE parent is null
        UNION ALL
          SELECT f.id, f.owner, f.title, f.parent, 
            p.path_ids || f.id,
            p.path_titles || f.title
          FROM folder f
          JOIN path p on f.parent = p.id
      )
      SELECT path_ids, path_titles FROM path WHERE id=?;
      """

    Option(sql.resultQuery(query, id).fetchOne).map { result => 
      val ids = result.getValue("path_ids", classOf[Array[UUID]]).toSeq
      val titles = result.getValue("path_titles", classOf[Array[String]]).toSeq
      ids.zip(titles).map(t => Breadcrumb(t._1, t._2))
    } getOrElse {
      Seq.empty[Breadcrumb]
    }
  }

  /** Gets the breadcrumb trail for a folder 'shared with me'.
    * 
    * Note that the trail can be different from the acual folder trail. In case 
    * the folder 
    */
  def getSharedWithMeBreadcrumbTrail(username: String, id: UUID) = db.query { sql => 
    val query = 
      """
      WITH RECURSIVE path AS (
        SELECT 
          sharing_policy.shared_with, 
          folder.id,
          ARRAY[folder.id] AS path_ids,
          ARRAY[folder.title] AS path_titles
        FROM sharing_policy
          JOIN folder ON folder.id = sharing_policy.folder_id
          LEFT OUTER JOIN folder parent_folder ON parent_folder.id = folder.parent
          LEFT OUTER JOIN sharing_policy parent_sharing_policy ON parent_sharing_policy.folder_id = parent_folder.id
        WHERE 
          sharing_policy.shared_with = ? AND
          parent_sharing_policy IS NULL

        UNION ALL

        SELECT 
          s.shared_with, 
          folder.id, 
          p.path_ids || folder.id,
          p.path_titles || folder.title
        FROM sharing_policy s
          JOIN folder ON folder.id = s.folder_id
          LEFT OUTER JOIN folder parent_folder ON parent_folder.id = folder.parent
          LEFT OUTER JOIN sharing_policy parent_sharing_policy ON parent_sharing_policy.folder_id = parent_folder.id
          JOIN path p on folder.parent = p.id
        WHERE 
          s.shared_with = ? AND
          parent_sharing_policy IS NULL
      )
      SELECT path_ids, path_titles FROM path WHERE id=?;
      """

    Option(sql.resultQuery(query, username, username, id).fetchOne).map { result => 
      val ids = result.getValue("path_ids", classOf[Array[UUID]]).toSeq
      val titles = result.getValue("path_titles", classOf[Array[String]]).toSeq
      ids.zip(titles).map(t => Breadcrumb(t._1, t._2))
    } getOrElse {
      Seq.empty[Breadcrumb]
    }
  }

}