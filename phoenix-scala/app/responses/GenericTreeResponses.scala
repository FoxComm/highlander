package responses

import models.tree.{GenericTree, GenericTreeNode}

object GenericTreeResponses {

  object FullTreeResponse {

    case class Root(tree: TreeResponse.Root, nodeValues: List[ResponseItem])

    def build(tree: TreeResponse.Root, nodeValues: List[ResponseItem]): Root =
      Root(tree, nodeValues)
  }

  object TreeResponse {

    case class Root(id: Int, name: String, contextId: Int, nodes: Node) extends ResponseItem

    def build(tree: GenericTree, nodes: Seq[GenericTreeNode]) =
      buildTree(nodes).map(nodes ⇒ Root(tree.id, tree.name, tree.contextId, nodes))

    case class Node(kind: String, objectId: Int, index: Int, children: Seq[Node])
        extends ResponseItem

    def buildTree(nodes: Seq[GenericTreeNode]): Option[TreeResponse.Node] = {
      buildTree(1, nodes.sortBy(_.path.value.size)).headOption
    }

    private def buildTree(level: Int, nodesSorted: Seq[GenericTreeNode]): Seq[TreeResponse.Node] = {
      val (heads, tail) = nodesSorted.span(_.path.value.size == level)
      heads.map(
        head ⇒
          Node(head.kind,
               head.objectId,
               head.index,
               buildTree(level + 1, tail.filter(_.path.value.startsWith(head.path.value)))))
    }
  }
}
