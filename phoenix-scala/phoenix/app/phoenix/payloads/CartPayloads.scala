package phoenix.payloads

import cats.data.ValidatedNel
import cats.implicits._
import core.utils.Validation
import core.failures.Failure
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload
import utils.Validation._

object CartPayloads {

  case class CreateCart(customerId: Option[Int] = None,
                        email: Option[String] = None,
                        scope: Option[String] = None)
      extends Validation[CreateCart] {

    def validate: ValidatedNel[Failure, CreateCart] = {

      (validExpr(customerId.isDefined || email.isDefined, "customerId or email must be given") |+|
            email.fold(ok)(notEmpty(_, "email"))).map(_ ⇒ this)
    }
  }

  case class CheckoutCart(items: List[UpdateLineItemsPayload])
}
