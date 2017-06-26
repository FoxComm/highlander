package phoenix.payloads

import phoenix.utils.aliases._

object TaxonomyPayloads {
  type AttributesMap = Map[String, Json]

  case class CreateTaxonomyPayload(attributes: AttributesMap,
                                   hierarchical: Boolean,
                                   scope: Option[String] = None)

  case class UpdateTaxonomyPayload(attributes: AttributesMap)
}
