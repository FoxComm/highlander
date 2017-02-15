package payloads

import utils.aliases._

object ObjectSchemaPayloads {
  case class CreateSchemaPayload(name: String,
                                 kind: String,
                                 schema: Json,
                                 contextId: Int,
                                 dependencies: List[String])

  case class UpdateSchemaPayload(schema: Option[Json], dependencies: Option[List[String]])
}
