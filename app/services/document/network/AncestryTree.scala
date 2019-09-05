package services.document.network

import java.sql.Timestamp

/** 'Flat' record structure, coming from the DB */
private[network] case class TreeRecord(
  id: String, 
  owner: String, 
  clonedFrom: Option[String] = None, 
  clonedAt: Option[Timestamp] = None)

/** A recursive tree structure, built lazily from the flat DB records **/
case class AncestryTree(private val root: TreeRecord, private[network] val descendants: Seq[TreeRecord]) {

  val rootNode = AncestryTreeNode(
    root.id, 
    root.owner, 
    root.clonedFrom, root.clonedAt, // Should ALWAYS be None
    this)

}

case class AncestryTreeNode(
  id: String, 
  owner: String, 
  clonedFrom: Option[String], 
  clonedAt: Option[Timestamp], 
  private val tree: AncestryTree
) {

  lazy val children: Seq[AncestryTreeNode] = 
    tree.descendants
      .filter(_.clonedFrom  == Some(id))
      .map(r => AncestryTreeNode(r.id, r.owner, r.clonedFrom, r.clonedAt, tree))
      
}
