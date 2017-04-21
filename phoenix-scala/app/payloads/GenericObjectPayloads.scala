package payloads

import utils.aliases._

object GenericObjectPayloads {

  case class CreateGenericObject(kind: String,
                                 attributes: Map[String, Json],
                                 schema: Option[String] = None,
                                 scope: Option[String] = None)

  case class UpdateGenericObject(attributes: Map[String, Json])
}
