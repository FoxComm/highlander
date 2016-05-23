package payloads

import java.time.Instant

import cats.data.ValidatedNel
import cats.implicits._
import failures.Failure
import models.order.Order.State
import utils.Validation

object OrderPayloads {

  case class UpdateOrderPayload(state: State)

  case class BulkUpdateOrdersPayload(referenceNumbers: Seq[String], state: State)

  case class CreateOrderNotePayload(body: String)

  case class CreateOrder(customerId: Option[Int] = None, email: Option[String] = None)
    extends Validation[CreateOrder] {

    def validate: ValidatedNel[Failure, CreateOrder] = {
      import Validation._

      (validExpr(customerId.isDefined || email.isDefined, "customerId or email must be given")
        |@| email.fold(ok)(notEmpty(_, "email"))
        ).map { case _ ⇒ this }
    }
  }

  case class OrderTimeMachine(referenceNumber: String, placedAt: Instant)

}
