package phoenix.failures

import core.failures.Failure
import phoenix.models.payment.creditcard._
import phoenix.models.payment.{ExternalCharge, ExternalChargeVals}

object CaptureFailures {

  case class SkuNotFoundInOrder(sku: String, refNum: String) extends Failure {
    override def description =
      s"line item with sku=$sku not found in order with referenceNumber=$refNum"
  }

  case class SkuMissingPrice(sku: String) extends Failure {
    override def description =
      s"The SKU $sku is missing a price"
  }

  case class ShippingCostNegative(total: Long) extends Failure {
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

  case class ChargeNotInAuth(charge: ExternalChargeVals) extends Failure {
    override def description =
      s"The charge ${charge.stripeChargeId} must be in Auth state. The charge is in ${charge.state} state."
  }

  case class ExternalPaymentNotFound(refNum: String) extends Failure {
    override def description =
      s"Unable to find any external payment for the order $refNum"
  }
}
