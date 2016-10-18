package payloads

import cats.data.ValidatedNel
import failures.Failure
import utils.Validation
import utils.aliases._

object TaxonomyPayloads {
  type AttributesMap = Map[String, Json]

  case class CreateTaxonomyPayload(attributes: AttributesMap,
                                   hierarchical: Boolean,
                                   scope: Option[String])

  case class UpdateTaxonomyPayload(attributes: AttributesMap)

  case class TaxonLocation(parent: Option[Int], position: Int) extends Validation[TaxonLocation] {
    override def validate: ValidatedNel[Failure, TaxonLocation] =
      Validation.greaterThanOrEqual(position, 0, "location.position").map(_ ⇒ this)
  }

  case class CreateTaxonPayload(attributes: AttributesMap,
                                location: Option[TaxonLocation],
                                scope: Option[String] = None)
      extends Validation[CreateTaxonPayload] {
    override def validate: ValidatedNel[Failure, CreateTaxonPayload] =
      location.map(_.validate).getOrElse(Validation.ok).map(_ ⇒ this)
  }

  case class UpdateTaxonPayload(attributes: Option[AttributesMap], location: Option[TaxonLocation])
      extends Validation[UpdateTaxonPayload] {
    override def validate: ValidatedNel[Failure, UpdateTaxonPayload] =
      location.map(_.validate).getOrElse(Validation.ok).map(_ ⇒ this)
  }
}
