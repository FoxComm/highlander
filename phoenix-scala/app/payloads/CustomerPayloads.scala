package payloads

import cats.data._
import cats.implicits._
import failures.Failure
import utils.Validation
import utils.Validation._

object CustomerPayloads {

  case class CreateCustomerPayload(email: String,
                                   name: Option[String] = None,
                                   password: Option[String] = None,
                                   isGuest: Option[Boolean] = Some(false),
                                   scope: Option[String] = None)

  case class UpdateCustomerPayload(name: Option[String] = None,
                                   email: Option[String] = None,
                                   phoneNumber: Option[String] = None)
      extends Validation[UpdateCustomerPayload] {

    def validate: ValidatedNel[Failure, UpdateCustomerPayload] = {
      (nullOrNotEmpty(name, "name") |@| nullOrNotEmpty(email, "email") |@| nullOrNotEmpty(
        phoneNumber,
        "phoneNumber")).map { case _ ⇒ this }
    }
  }

  case class ChangeCustomerPasswordPayload(oldPassword: String, newPassword: String)
      extends Validation[ChangeCustomerPasswordPayload] {
    def validate: ValidatedNel[Failure, ChangeCustomerPasswordPayload] =
      notEmpty(newPassword, "new password").map { case _ ⇒ this }
  }

  case class ActivateCustomerPayload(name: String) extends Validation[ActivateCustomerPayload] {

    def validate: ValidatedNel[Failure, ActivateCustomerPayload] =
      notEmpty(name, "name").map { case _ ⇒ this }
  }

  case class CustomerSearchForNewOrder(term: String)
      extends Validation[CustomerSearchForNewOrder] {

    def validate: ValidatedNel[Failure, CustomerSearchForNewOrder] =
      greaterThan(term.length, 1, "term size").map { case _ ⇒ this }
  }

}
