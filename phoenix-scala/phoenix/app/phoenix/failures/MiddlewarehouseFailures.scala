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

  case object MwhConnectionFailure extends Failure {
    override def description =
      s"We are experiencing problems and doing our best to resolve the issue. Please try again later."
  }

  case object UnexpectedMwhResponseFailure extends Failure {
    override def description = "Unexpected error occurred while creating SKU. Please contact administrator."
  }
}
