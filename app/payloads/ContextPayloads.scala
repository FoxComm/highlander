package payloads

import models.Aliases.Json
import java.time.Instant

case class CreateObjectContext(name: String, attributes: Json)
case class UpdateObjectContext(name: String, attributes: Json)
