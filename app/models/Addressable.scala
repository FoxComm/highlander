package models

import cats.data.Validated.valid
import cats.data.ValidatedNel
import monocle.Lens
import utils.Validation.{matches ⇒ matchesNew, notEmpty ⇒ notEmptyNew}
import utils.Litterbox._
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

  def validateNew: ValidatedNel[String, M] = {
    val isUsAddress = Country.usRegions.contains(regionId)

    val phone: ValidatedNel[String, Unit] = (isUsAddress, phoneNumber) match {
      case (true, Some(number)) ⇒
        matchesNew(number, "[0-9]{10}", "phoneNumber")
      case (false, Some(number)) ⇒
        matchesNew(number, "[0-9]{0,15}", "phoneNumber")
      case (_, None) ⇒
        valid({})
    }

    val zipValidation: ValidatedNel[String, Unit] = (isUsAddress, zip) match {
      case (true, zipValue) ⇒
        matchesNew(zipValue, Address.zipPatternUs, "zip")
      case (false, zipValue) ⇒
        matchesNew(zipValue, Address.zipPattern, "zip")
    }

    ( notEmptyNew(name, "name")
      |@| notEmptyNew(street1, "street1")
      |@| notEmptyNew(city, "city")
      |@| zipValidation
      |@| phone
      ).map { case _ ⇒ instance }
  }
}
