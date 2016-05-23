package payloads

import models.Aliases.Json

object ContextPayloads {

  case class CreateObjectContext(name: String, attributes: Json)

  case class UpdateObjectContext(name: String, attributes: Json)

}
