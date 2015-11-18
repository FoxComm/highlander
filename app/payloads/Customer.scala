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

  def validate: ValidatedNel[Failure, UpdateCustomerPayload] = {
    (nullOrNotEmpty(name, "name")
      |@| nullOrNotEmpty(email, "email")
      |@| nullOrNotEmpty(phoneNumber, "phoneNumber")
      ).map { case _ ⇒ this }
  }
}

final case class ActivateCustomerPayload(name: String)
  extends Validation[ActivateCustomerPayload] {

  def validate: ValidatedNel[Failure, ActivateCustomerPayload] =
    notEmpty(name, "name").map { case _ ⇒ this }
}

final case class ToggleCustomerDisabled(disabled: Boolean)
// TODO: add blacklistedReason later
final case class ToggleCustomerBlacklisted(blacklisted: Boolean)

final case class CustomerSearchForNewOrder(term: String)
  extends Validation[CustomerSearchForNewOrder] {

  def validate: ValidatedNel[Failure, CustomerSearchForNewOrder] =
    greaterThan(term.size, 1, "term size").map { case _ ⇒ this }
}

