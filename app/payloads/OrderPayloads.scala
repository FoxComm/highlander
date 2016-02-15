package payloads

import cats.data.ValidatedNel
import cats.implicits._
import models.order.Order
import utils.Litterbox._
import Order.State
import services.{GeneralFailure, Failure}
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

final case class Assignment(assignees: Seq[Int])

final case class BulkAssignment(referenceNumbers: Seq[String], assigneeId: Int)

final case class Watchers(watchers: Seq[Int])

final case class BulkWatchers(referenceNumbers: Seq[String], watcherId: Int)