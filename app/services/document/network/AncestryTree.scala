package services.document.network

/** 'Flat' record structure, coming from the DB */
private[network] case class TreeRecord(id: String, owner: String, clonedFrom: Option[String] = None)

/** A recursive tree structure, built lazily from the flat DB records **/
case class AncestryTree(private val root: TreeRecord, private[network] val descendants: Seq[TreeRecord]) {

  val rootNode = TreeNode(
    root.id, 
    root.owner, 
    root.clonedFrom, // Should ALWAYS be None
    this)

}

case class TreeNode(id: String, owner: String, clonedFrom: Option[String], private val tree: AncestryTree) {

  lazy val children: Seq[TreeNode] = 
    tree.descendants
      .filter(_.clonedFrom  == Some(id))
      .map(r => TreeNode(r.id, r.owner, r.clonedFrom, tree))
      
}
