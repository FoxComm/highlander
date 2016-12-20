package failures

import utils.friendlyClassName

import models.payment.creditcard._

object CaptureFailures {

  case class VariantNotFoundInOrder(variant: String, refNum: String) extends Failure {
    override def description =
      s"line item with variant=$variant not found in order with referenceNumber=$refNum"
  }

  case class VariantMissingPrice(variant: String) extends Failure {
    override def description =
      s"The Variant $variant is missing a price"
  }

  case class ShippingCostNegative(total: Int) extends Failure {
    override def description =
      s"Expected a shipping cost greater than zero but got $total"
  }

  case class SplitCaptureNotSupported(refNum: String) extends Failure {
    override def description =
      s"Split capture is not supported for order $refNum. Please provide all line items in order."
  }

  case class StripeChargeForOrderNotFound(refNum: String) extends Failure {
    override def description =
      s"Stripe charge for order $refNum is not found."
  }

  case class OrderMustBeInAuthState(refNum: String) extends Failure {
    override def description =
      s"Order $refNum is not in Auth state."
  }

  case class ChargeNotInAuth(charge: CreditCardCharge) extends Failure {
    override def description =
      s"The charge ${charge.chargeId} must be in Auth state. The charge is in ${charge.state} state."
  }

  case class CreditCardNotFound(refNum: String) extends Failure {
    override def description =
      s"Unable to find a credit card for the order $refNum"
  }
}
