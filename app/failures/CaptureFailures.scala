package failures

import utils.friendlyClassName

object CaptureFailures {

  case class SkuNotFoundInOrder(sku: String, refNum: String) extends Failure {
    override def description =
      s"line item with sku=$sku not found in order with referenceNumber=$refNum"
  }

  case class SkuMissingPrice(sku: String) extends Failure {
    override def description =
      s"The SKU $sku is missing a price"
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

  case class OrderMustBeInFullimentStarted(refNum: String) extends Failure {
    override def description =
      s"Order $refNum is not in Fullillment Started state."
  }

  case class ChargeNotInAuth(chargeId: String, chargeState: String) extends Failure {
    override def description =
      s"The charge $chargeId must be in Auth state. The charge is in $chargeState state."
  }
}
