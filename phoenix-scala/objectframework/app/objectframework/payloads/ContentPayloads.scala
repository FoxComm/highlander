package objectframework.payloads

import objectframework.content._

object ContentPayloads {
  // TODO: At some point, make this is a trait so that we can implement payloads
  // with this contract that can be understood by ContentManager.
  case class CreateContentPayload(viewId: Int,
                                  kind: String,
                                  attributes: Content.ContentAttributes,
                                  relations: Content.ContentRelations)
}
