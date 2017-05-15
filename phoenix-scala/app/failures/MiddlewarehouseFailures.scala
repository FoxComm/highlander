package failures

object MiddlewarehouseFailures {

  case class MiddlewarehouseError(message: String) extends Failure {
    override def description = message
  }

  case class SkusOutOfStockFailure(skus: List[String]) extends Failure {
    val readableSkus = skus.mkString(", ")

    override def description =
      s"Following SKUs are out of stock: $readableSkus. Please remove them from your cart to complete checkout."
  }

  case object UnableToHoldLineItems extends Failure {
    override def description = s"Unable to hold line items"
  }

  case object UnableToCancelHoldLineItems extends Failure {
    override def description = s"Unable to cancel hold on line items"
  }

}
