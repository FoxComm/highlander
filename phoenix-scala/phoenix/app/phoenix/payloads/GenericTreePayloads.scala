package phoenix.payloads

object GenericTreePayloads {
  case class NodePayload(kind: String, objectId: Int, children: List[NodePayload])

  case class NodeWithIndexPayload(index: Int,
                                  kind: String,
                                  objectId: Int,
                                  children: List[NodeWithIndexPayload])

  case class MoveNodePayload(index: Option[Int], child: Int)

  case class NodeValuesPayload(kind: String, objectId: Int)
}
