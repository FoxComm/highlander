package payloads

import utils.aliases._

object TaxonomyPayloads {
  case class CreateTaxonPayload(attributes: Map[String, Json], hierarchical: Boolean)

  case class UpdateTaxonPayload(attributes: Map[String, Json])

  case class CreateTermPayload(taxonId: Int,
                               attributes: Map[String, Json],
                               parent: Option[Int],
                               sibling: Option[Int])

  case class UpdateTermPayload(attributes: Option[Map[String, Json]],
                               parent: Option[Int],
                               sibling: Option[Int])
}
