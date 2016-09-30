package payloads

import cats.data.ValidatedNel
import failures.Failure
import services.taxonomy.TaxonomyValidation
import utils.Validation
import utils.aliases._

object TaxonomyPayloads {
  type AttributesMap = Map[String, Json]

  case class CreateTaxonomyPayload(attributes: AttributesMap, hierarchical: Boolean)

  case class UpdateTaxonomyPayload(attributes: AttributesMap)

  case class CreateTaxonPayload(attributes: AttributesMap,
                                parent: Option[Int],
                                sibling: Option[Int])
      extends Validation[CreateTaxonPayload] {
    override def validate: ValidatedNel[Failure, CreateTaxonPayload] =
      TaxonomyValidation.validateParentOrSiblingIsDefined(parent, sibling).map { case _ ⇒ this }
  }

  case class UpdateTaxonPayload(attributes: Option[AttributesMap],
                                parent: Option[Int],
                                sibling: Option[Int])
      extends Validation[UpdateTaxonPayload] {
    override def validate: ValidatedNel[Failure, UpdateTaxonPayload] =
      TaxonomyValidation.validateParentOrSiblingIsDefined(parent, sibling).map { case _ ⇒ this }
  }
}
