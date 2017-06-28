package phoenix.responses

import phoenix.models.tree.{GenericTree, GenericTreeNode}

object GenericTreeResponses {

  case class FullTreeResponse(tree: TreeResponse, nodeValues: List[ResponseItem])

  object FullTreeResponse {

    def build(tree: TreeResponse, nodeValues: List[ResponseItem]): FullTreeResponse =
      FullTreeResponse(tree, nodeValues)
  }

  case class TreeNodeResponse(kind: String, objectId: Int, index: Int, children: Seq[TreeNodeResponse])
      extends ResponseItem

  object TreeNodeResponse {
    def buildTree(nodes: Seq[GenericTreeNode]): Option[TreeNodeResponse] =
      buildTree(1, nodes.sortBy(_.path.value.size)).headOption

    private def buildTree(level: Int, nodesSorted: Seq[GenericTreeNode]): Seq[TreeNodeResponse] = {
      val (heads, tail) = nodesSorted.span(_.path.value.size == level)
      heads.map { head ⇒
        TreeNodeResponse(head.kind,
                         head.objectId,
                         head.index,
                         buildTree(level + 1, tail.filter(_.path.value.startsWith(head.path.value))))
      }
    }
  }

  case class TreeResponse(id: Int, name: String, contextId: Int, nodes: TreeNodeResponse) extends ResponseItem

  object TreeResponse {
    def build(tree: GenericTree, nodes: Seq[GenericTreeNode]): Option[TreeResponse] =
      TreeNodeResponse.buildTree(nodes).map(nodes ⇒ TreeResponse(tree.id, tree.name, tree.contextId, nodes))
  }
}
