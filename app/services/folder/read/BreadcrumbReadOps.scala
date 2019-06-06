package services.folder.read

import java.util.UUID
import org.jooq.DSLContext
import scala.concurrent.ExecutionContext
import services.folder.{Breadcrumb, FolderService}

trait BreadcrumbReadOps { self: FolderService => 

  /** Tests if folder A is a child of folder B, i.e. if A is somewhere down the hierarchy chain from B **/
  def isChildOf(folderA: UUID, folderB: UUID)(implicit ctx: ExecutionContext) =
    getBreadcrumbs(folderA).map { breadcrumbs => 
      breadcrumbs.exists(_.id == folderB)  
    }

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

  /** Accessible docs breadcrumbs for an anonymous (= not logged in) visitor  **/
  private def queryAccessibleForAnonymousVisitor(owner: String, id: UUID, sql: DSLContext) = {
    val q = 
      """
      WITH RECURSIVE path AS (
        SELECT 
          folder.id,
          folder.public_visibility,
          ARRAY[folder.id] AS path_ids,
          ARRAY[folder.title] AS path_titles
        FROM folder
          LEFT OUTER JOIN folder parent_folder ON parent_folder.id = folder.parent
        WHERE 
          folder.owner = ?
            AND
          folder.public_visibility = 'PUBLIC'
            AND
          (parent_folder.public_visibility IS NULL OR NOT parent_folder.public_visibility = 'PUBLIC')

        UNION ALL

        SELECT 
          f.id, 
          f.public_visibility,
          p.path_ids || f.id,
          p.path_titles || f.title
        FROM folder f
          LEFT OUTER JOIN folder parent_folder ON parent_folder.id = f.parent
          JOIN path p on f.parent = p.id
        WHERE 
          f.owner = ?
            AND
          f.public_visibility = 'PUBLIC'
            AND
          NOT parent_folder.public_visibility = 'PUBLIC'
      )
      SELECT path_ids, path_titles FROM path WHERE id = ?;
      """

    sql.resultQuery(q, owner, owner, id)
  }

  private def queryAccessibleForLoggedInVisitor(owner: String, visitor: String, id: UUID, sql: DSLContext) = {
    val q = 
      """
      WITH RECURSIVE path AS (
        SELECT 
          folder.id,
          folder.public_visibility,
          sharing_policy.shared_with, 
          ARRAY[folder.id] AS path_ids,
          ARRAY[folder.title] AS path_titles
        FROM folder
          LEFT OUTER JOIN sharing_policy on sharing_policy.folder_id = folder.id
          LEFT OUTER JOIN folder parent_folder ON parent_folder.id = folder.parent
          LEFT OUTER JOIN sharing_policy parent_sharing_policy ON parent_sharing_policy.folder_id = parent_folder.id
        WHERE 
          folder.owner = ?
	          AND
          (folder.public_visibility = 'PUBLIC' OR sharing_policy.shared_with = ?)
            AND
          (parent_folder.public_visibility IS NULL 
             OR 
           NOT parent_folder.public_visibility = 'PUBLIC' 
             AND parent_sharing_policy IS NULL)

        UNION ALL

        SELECT 
          folder.id, 
          folder.public_visibility,
          s.shared_with, 
          p.path_ids || folder.id,
          p.path_titles || folder.title
        FROM sharing_policy s
          JOIN folder ON folder.id = s.folder_id
          LEFT OUTER JOIN folder parent_folder ON parent_folder.id = folder.parent
          LEFT OUTER JOIN sharing_policy parent_sharing_policy ON parent_sharing_policy.folder_id = parent_folder.id
          JOIN path p on folder.parent = p.id
        WHERE 
          folder.owner = ?
            AND
          (s.shared_with = ? OR folder.public_visibility = 'PUBLIC')
            AND
          (parent_sharing_policy IS NULL AND NOT parent_folder.public_visibility = 'PUBLIC')
      )
      SELECT path_ids, path_titles FROM path WHERE id = ?;
      """

    sql.resultQuery(q, owner, visitor, owner, visitor, id)
  }

  def getAccessibleDocsBreadcrumbTrail(owner: String, loggedInAs: Option[String], id: UUID) = db.query { sql =>
    val query = loggedInAs match {
      case Some(visitor) => queryAccessibleForLoggedInVisitor(owner, visitor, id, sql)
      case None => queryAccessibleForAnonymousVisitor(owner, id, sql)
    }

    Option(query.fetchOne).map { result => 
      val ids = result.getValue("path_ids", classOf[Array[UUID]]).toSeq
      val titles = result.getValue("path_titles", classOf[Array[String]]).toSeq
      ids.zip(titles).map(t => Breadcrumb(t._1, t._2))
    } getOrElse {
      Seq.empty[Breadcrumb]
    }
  }

}