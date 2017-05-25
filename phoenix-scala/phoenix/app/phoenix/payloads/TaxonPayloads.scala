package phoenix.payloads

import cats.data.ValidatedNel
import core.utils.Validation
import core.failures.Failure
import phoenix.utils.aliases._

object TaxonPayloads {
  type AttributesMap = Map[String, Json]

  case class TaxonLocationPayload(parent: Option[Int], position: Option[Int])
      extends Validation[TaxonLocationPayload] {
    override def validate: ValidatedNel[Failure, TaxonLocationPayload] =
      position
        .fold(Validation.ok)(Validation.greaterThanOrEqual(_, 0, "location.position"))
        .map(_ ⇒ this)
  }

  case class CreateTaxonPayload(attributes: AttributesMap,
                                location: Option[TaxonLocationPayload],
                                scope: Option[String] = None)
      extends Validation[CreateTaxonPayload] {
    override def validate: ValidatedNel[Failure, CreateTaxonPayload] =
      location.map(_.validate).getOrElse(Validation.ok).map(_ ⇒ this)
  }

  case class UpdateTaxonPayload(attributes: AttributesMap, location: Option[TaxonLocationPayload])
      extends Validation[UpdateTaxonPayload] {
    override def validate: ValidatedNel[Failure, UpdateTaxonPayload] =
      location.map(_.validate).getOrElse(Validation.ok).map(_ ⇒ this)
  }
}
