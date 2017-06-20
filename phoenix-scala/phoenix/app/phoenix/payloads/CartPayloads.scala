package phoenix.payloads

import cats.data.ValidatedNel
import cats.implicits._
import core.failures.Failure
import core.utils.Validation
import core.utils.Validation._
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload

object CartPayloads {

  case class CreateCart(customerId: Option[Int] = None,
                        email: Option[String] = None,
                        scope: Option[String] = None)
      extends Validation[CreateCart] {

    def validate: ValidatedNel[Failure, CreateCart] =
      (validExpr(customerId.isDefined || email.isDefined, "customerId or email must be given") |+|
        email.fold(ok)(notEmpty(_, "email"))).map(_ ⇒ this)
  }

  case class CheckoutCart(items: List[UpdateLineItemsPayload])
}
