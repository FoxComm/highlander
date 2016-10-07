package payloads

import utils.aliases._

object ContextPayloads {

  case class CreateObjectContext(name: String, attributes: Json)

  case class UpdateObjectContext(name: String, attributes: Json)
}
