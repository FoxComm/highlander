package models.traits

import cats.data.ValidatedNel
import cats.implicits._
import failures.Failure
import models.traits.CreditCardValidations._
import utils.Validation
import utils.Validation._

trait CreditCardBase[A] extends Validation[A] { self: A ⇒

  def expMonth: Int
  def expYear: Int
  def lastFour: String
  def holderName: String

  def validate: ValidatedNel[Failure, A] =
    (validExpDate(expMonth, expYear) |@| validHolderName(holderName) |@| validLastFour(lastFour)).map {
      case _ ⇒ this
    }
}

object CreditCardValidations {

  def validExpDateUpdate(oldExpMonth: Int,
                         oldExpYear: Int,
                         newExpMonth: Option[Int],
                         newExpYear: Option[Int]): ValidatedNel[Failure, Unit] =
    (newExpMonth, newExpYear) match {
      case (Some(newMonth), Some(newYear)) ⇒ validExpDate(newMonth, newYear)
      case (Some(newMonth), _)             ⇒ validExpDate(newMonth, oldExpYear)
      case (_, Some(newYear))              ⇒ validExpDate(oldExpMonth, newYear)
      case _                               ⇒ validExpDate(oldExpMonth, oldExpYear)
    }

  def validExpMonth(expMonth: Int): ValidatedNel[Failure, Unit] =
    isMonth(expMonth, "expiration")

  def validExpYear(expYear: Int): ValidatedNel[Failure, Unit] =
    withinTwentyYears(expYear, "expiration")

  def validExpDate(expMonth: Int, expYear: Int): ValidatedNel[Failure, Unit] = {
    val cardNotExpired = notExpired(expYear, expMonth, "credit card is expired")
    (validExpMonth(expMonth) |@| validExpYear(expYear) |@| cardNotExpired).map { case _ ⇒ {} }
  }

  def validLastFour(lastFour: String): ValidatedNel[Failure, Unit] =
    matches(lastFour, "[0-9]{4}", "last four")

  def validHolderName(holderName: String): ValidatedNel[Failure, Unit] =
    notEmpty(holderName, "holder name")

  def validCardToken(token: String): ValidatedNel[Failure, Unit] =
    matches(token, "tok_\\w*", "credit card token")

  def validCardBrand(brand: String): ValidatedNel[Failure, Unit] =
    notEmpty(brand, "credit card brand")

  def validCardNumber(cardNumber: String): ValidatedNel[Failure, Unit] =
    matches(cardNumber, "[0-9]+", "number")

  def validCvv(cvv: String): ValidatedNel[Failure, Unit] =
    matches(cvv, "[0-9]{3,4}", "cvv")
}
