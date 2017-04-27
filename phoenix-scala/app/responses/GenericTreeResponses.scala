package responses

import io.circe.syntax._
import io.circe.{Encoder, Json}
import models.tree.{GenericTree, GenericTreeNode}
import utils.aliases.Json
import utils.json.codecs._

object GenericTreeResponses {

  object FullTreeResponse {

    case class Root(tree: TreeResponse.Root, nodeValues: List[ResponseItem])
    object Root {
      implicit val encodeRoot: Encoder[Root] = new Encoder[Root] {
        def apply(a: Root): Json = Json.obj(
            "tree"       → Encoder[TreeResponse.Root].apply(a.tree),
            "nodeValues" → Json.fromValues(a.nodeValues.map(_.json))
        )
      }
    }

    def build(tree: TreeResponse.Root, nodeValues: List[ResponseItem]): Root =
      Root(tree, nodeValues)
  }

  object TreeResponse {

    case class Root(id: Int, name: String, contextId: Int, nodes: Node) extends ResponseItem {
      def json: Json = this.asJson
    }

    def build(tree: GenericTree, nodes: Seq[GenericTreeNode]) =
      buildTree(nodes).map(nodes ⇒ Root(tree.id, tree.name, tree.contextId, nodes))

    case class Node(kind: String, objectId: Int, index: Int, children: Seq[Node])
        extends ResponseItem {
      def json: Json = this.asJson
    }

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
