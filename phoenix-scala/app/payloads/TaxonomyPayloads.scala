package payloads

import utils.aliases._

object TaxonomyPayloads {
  type AttributesMap = Map[String, Json]

  case class CreateTaxonomyPayload(attributes: AttributesMap, hierarchical: Boolean)

  case class UpdateTaxonomyPayload(attributes: AttributesMap)

  case class TaxonLocation(parent: Option[Int], position: Int)

  case class CreateTaxonPayload(attributes: AttributesMap, location: Option[TaxonLocation])

  case class UpdateTaxonPayload(attributes: Option[AttributesMap], location: Option[TaxonLocation])
}
