package phoenix.models.traits

import cats.data.Validated.valid
import cats.data.ValidatedNel
import cats.implicits._
import core.utils.Validation
import core.failures.Failure
import phoenix.models.location.{Address, Region}
import shapeless._

trait Addressable[M] { self: M ⇒
  import Validation._

  def city: String
  def name: String
  def phoneNumber: Option[String]
  def regionId: Int
  def address1: String
  def address2: Option[String]
  def zip: String

  def zipLens: Lens[M, String]

  def sanitize(model: M): M =
    if (Region.usRegions.contains(regionId))
      zipLens.set(model)(zip.replace("-", ""))
    else
      model

  def validate: ValidatedNel[Failure, M] = {
    val isUsAddress = Region.usRegions.contains(regionId)

    val phone: ValidatedNel[Failure, Unit] = (isUsAddress, phoneNumber) match {
      case (true, Some(number)) ⇒
        matches(number, "[0-9]{10}", "phoneNumber")
      case (false, Some(number)) ⇒
        matches(number, "[0-9]{0,15}", "phoneNumber")
      case (_, None) ⇒
        valid({})
    }

    val zipValidation: ValidatedNel[Failure, Unit] = (isUsAddress, zip) match {
      case (true, zipValue) ⇒
        matches(zipValue, Address.zipPatternUs, "zip")
      case (false, zipValue) ⇒
        matches(zipValue, Address.zipPattern, "zip")
    }

    (notEmpty(name, "name") |@| notEmpty(address1, "address1") |@| notEmpty(city, "city") |@| zipValidation |@| phone)
      .map {
        case _ ⇒ this
      }
  }
}
