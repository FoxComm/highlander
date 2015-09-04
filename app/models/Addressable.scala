package models

import cats.data.ValidatedNel
import services.Failure
import utils.Litterbox._
import utils.Validation

import cats.data.Validated.valid
import monocle.Lens
import cats.syntax.apply._

trait Addressable[M] {
  import Validation._

  def city: String
  def name: String
  def phoneNumber: Option[String]
  def regionId: Int
  def street1: String
  def street2: Option[String]
  def zip: String

  def instance: M

  def zipLens: Lens[M, String]

  def sanitize(model: M): M = {
    if (Country.usRegions.contains(regionId)) {
      zipLens.set(zip.replace("-", ""))(model)
    } else {
      zipLens.set(zip)(model)
    }
  }

  def validate: ValidatedNel[Failure, M] = {
    val isUsAddress = Country.usRegions.contains(regionId)

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

    ( notEmpty(name, "name")
      |@| notEmpty(street1, "street1")
      |@| notEmpty(city, "city")
      |@| zipValidation
      |@| phone
      ).map { case _ ⇒ instance }
  }
}
