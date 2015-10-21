package payloads

import cats.data._
import services.Failure
import utils.Validation
import Validation._
import utils.Litterbox._
import cats.implicits._

final case class CreateCustomerPayload(email: String,
  name: Option[String] = None,
  password: Option[String] = None,
  isGuest: Option[Boolean] = Some(false))

final case class UpdateCustomerPayload(
    name: Option[String] = None,
    email: Option[String] = None,
    phoneNumber: Option[String] = None)
  extends Validation[UpdateCustomerPayload] {

  /* Fields can be None but provided value can't be empty
   */
  def validate: ValidatedNel[Failure, UpdateCustomerPayload] = {
    (notEmpty(name.getOrElse("1"), "name")
      |@| notEmpty(email.getOrElse("1"), "email")
      |@| notEmpty(phoneNumber.getOrElse("1"), "phoneNumber")
      ).map { case _ ⇒ this }
  }
}

final case class ToggleCustomerDisabled(disabled: Boolean)

