package payloads

import models.Aliases.Json
import java.time.Instant

final case class CreateObjectContext(name: String, attributes: Json)
final case class UpdateObjectContext(name: String, attributes: Json)
