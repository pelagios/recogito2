package services.document.network

case class AncestryTreeNode(id: String, owner: String, clonedFrom: Option[String] = None)

case class AncestryTree(root: AncestryTreeNode, descendants: Seq[AncestryTreeNode])
