package phoenix.failures

import core.failures.Failure

object MiddlewarehouseFailures {

  case class MiddlewarehouseError(message: String) extends Failure {
    override def description = message
  }

  case class SkusOutOfStockFailure(skus: List[String]) extends Failure {
    override def description =
      s"Following SKUs are out of stock: ${skus.mkString(", ")}. Please remove them from your cart to complete checkout."
  }

  case object UnableToHoldLineItems extends Failure {
    override def description = s"Unable to hold line items"
  }

  case object UnableToCancelHoldLineItems extends Failure {
    override def description = s"Unable to cancel hold on line items"
  }

  case object UnableToCreateSku extends Failure {
    override def description = "Unable to create SKU"
  }

  object NoSkuIdInResponse extends Failure {
    override def description = "No SKU ID found in Middlewarehouse response"
  }

  object UnableToParseResponse extends Failure {
    override def description = "Unable to parse Middlewarehouse response as JSON"
  }

}
