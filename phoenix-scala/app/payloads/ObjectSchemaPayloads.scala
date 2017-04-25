package payloads

import utils.aliases._

object ObjectSchemaPayloads {

  case class UpdateObjectSchema(schema: Json, dependencies: Option[List[String]])
}
