package phoenix.failures

import core.failures.Failure
import phoenix.utils.apis.MwhErrorInfo

object MiddlewarehouseFailures {

  case class MiddlewarehouseError(message: String) extends Failure {
    override def description = message
  }

  case class SkusOutOfStockFailure(skusErrors: List[MwhErrorInfo]) extends Failure {

    override def description = {
      def errorWhenListNotEmpty(error: String, skus: List[MwhErrorInfo]): String = {
        val skusMsg = skus.map(_.sku).mkString(", ")
        if (skusMsg.isEmpty()) "" else error + skusMsg + ". "
      }
      errorWhenListNotEmpty("There is not enough items in inventory for SKUs: ",
                            skusErrors.filter(_.afs != 0)) +
      errorWhenListNotEmpty("Following SKUs are out of stock: ", skusErrors.filter(_.afs == 0)) +
      "Update your cart in order to complete checkout."
    }
  }

  case object MwhConnectionFailure extends Failure {
    override def description =
      s"We are experiencing problems and doing our best to resolve the issue. Please try again later."
  }

  case object UnexpectedMwhResponseFailure extends Failure {
    override def description = "Unexpected error occurred while creating SKU. Please contact administrator."
  }
}
