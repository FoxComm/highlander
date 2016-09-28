package payloads

import utils.aliases._

object TaxonomyPayloads {
  case class CreateTaxonomyPayload(attributes: Map[String, Json], hierarchical: Boolean)

  case class UpdateTaxonomyPayload(attributes: Map[String, Json])

  case class CreateTaxonPayload(attributes: Map[String, Json],
                                parent: Option[Int],
                                sibling: Option[Int])

  case class UpdateTaxonPayload(attributes: Option[Map[String, Json]],
                                parent: Option[Int],
                                sibling: Option[Int])
}
