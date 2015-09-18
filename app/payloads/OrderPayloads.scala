package payloads

import cats.data.ValidatedNel
import cats.implicits._
import utils.Litterbox._
import models.Order.Status
import services.{GeneralFailure, Failure}
import utils.Validation

final case class UpdateOrderPayload(status: Status)

final case class BulkUpdateOrdersPayload(referenceNumbers: Seq[String], status: Status)

final case class CreateOrderNotePayload(body: String)

final case class CreateShippingAddress(addressId: Option[Int] = None, address: Option[CreateAddressPayload] = None)

final case class UpdateShippingAddress(addressId: Option[Int] = None, address: Option[UpdateAddressPayload] = None)

final case class CreateOrder(customerId: Option[Int] = None, email: Option[String] = None)
  extends Validation[CreateOrder] {

  def validate: ValidatedNel[Failure, CreateOrder] = {
    import Validation._

    ( validExpr(customerId.isDefined || email.isDefined, "customerId or email must be given")
      |@| email.fold(ok)(notEmpty(_, "email"))
    ).map { case _ ⇒ this }
  }
}

final case class Assignment(assignees: Seq[Int])
