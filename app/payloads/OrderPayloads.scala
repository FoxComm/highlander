package payloads

import cats.data.ValidatedNel
import cats.implicits._
import models.order.Order
import utils.Litterbox._
import Order.State
import failures.Failure
import utils.Validation

final case class UpdateOrderPayload(state: State)

final case class BulkUpdateOrdersPayload(referenceNumbers: Seq[String], state: State)

final case class CreateOrderNotePayload(body: String)

final case class CreateOrder(customerId: Option[Int] = None, email: Option[String] = None)
  extends Validation[CreateOrder] {

  def validate: ValidatedNel[Failure, CreateOrder] = {
    import Validation._

    ( validExpr(customerId.isDefined || email.isDefined, "customerId or email must be given")
      |@| email.fold(ok)(notEmpty(_, "email"))
    ).map { case _ ⇒ this }
  }
}
