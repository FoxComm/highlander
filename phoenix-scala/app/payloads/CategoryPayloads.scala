package payloads

import cats.data.ValidatedNel
import failures.Failure
import utils.Validation

object CategoryPayloads {
  case class Location(parent: Option[Int], position: Option[Int]) extends Validation[Location] {
    override def validate: ValidatedNel[Failure, Location] =
      position
        .fold(Validation.ok)(Validation.greaterThanOrEqual(_, 0, "location.position"))
        .map(_ ⇒ this)
  }

  case class CreateCategoryPayload(name: String,
                                   location: Option[Location],
                                   scope: Option[String] = None)
      extends Validation[CreateCategoryPayload] {
    override def validate: ValidatedNel[Failure, CreateCategoryPayload] =
      location.map(_.validate).getOrElse(Validation.ok).map(_ ⇒ this)
  }

  case class UpdateCategoryPayload(name: Option[String], location: Option[Location])
      extends Validation[UpdateCategoryPayload] {
    override def validate: ValidatedNel[Failure, UpdateCategoryPayload] =
      location.map(_.validate).getOrElse(Validation.ok).map(_ ⇒ this)
  }
}
