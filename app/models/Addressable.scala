package models

import cats.data.ValidatedNel
import services.Failure
import utils.Litterbox._
import utils.Validation

import cats.data.Validated.valid
import monocle.Lens
import cats.syntax.apply._

trait Addressable[M] {
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

  def validateNew: ValidatedNel[Failure, M] = {
    val isUsAddress = Country.usRegions.contains(regionId)

    val phone: ValidatedNel[Failure, Unit] = (isUsAddress, phoneNumber) match {
      case (true, Some(number)) ⇒
        Validation.matches(number, "[0-9]{10}", "phoneNumber")
      case (false, Some(number)) ⇒
        Validation.matches(number, "[0-9]{0,15}", "phoneNumber")
      case (_, None) ⇒
        valid({})
    }

    val zipValidation: ValidatedNel[Failure, Unit] = (isUsAddress, zip) match {
      case (true, zipValue) ⇒
        Validation.matches(zipValue, Address.zipPatternUs, "zip")
      case (false, zipValue) ⇒
        Validation.matches(zipValue, Address.zipPattern, "zip")
    }

    ( Validation.notEmpty(name, "name")
      |@| Validation.notEmpty(street1, "street1")
      |@| Validation.notEmpty(city, "city")
      |@| zipValidation
      |@| phone
      ).map { case _ ⇒ instance }
  }
}
