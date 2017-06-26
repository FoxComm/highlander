package phoenix.failures

import core.failures.{Failure, NotFoundFailure400}
import core.utils.Money.Currency
import core.utils.friendlyClassName
import phoenix.models.cord.Order

object ReturnFailures {

  case class OrderMustBeShippedForReturn(refNum: String, state: Order.State) extends Failure {
    def description: String =
      s"Cannot create return for order $refNum with $state. Order must be in ${Order.Shipped} state."
  }

  case class NoReturnsFoundForOrder(refNum: String) extends Failure {
    override def description = s"No return for order $refNum was found"
  }

  case class EmptyReturn(refNum: String) extends Failure {
    override def description = s"Return with referenceNumber=$refNum has no line items"
  }

  object ReturnPaymentNotFoundFailure {
    def apply[M](m: M): NotFoundFailure400 =
      NotFoundFailure400(s"${friendlyClassName(m)} payment not found")
  }

  case class ReturnShippingCostExceeded(refNum: String, amount: Long, maxAmount: Long) extends Failure {
    def description: String =
      s"Returned shipping cost ($amount) cannot be greater than $maxAmount for return $refNum"
  }

  case class ReturnSkuItemQuantityExceeded(refNum: String, quantity: Int, maxQuantity: Int) extends Failure {
    def description: String =
      s"Returned sku line item quantity ($quantity) cannot be greater than $maxQuantity for return $refNum"
  }

  case class ReturnPaymentExceeded(refNum: String, amount: Long, maxAmount: Long) extends Failure {
    def description: String =
      s"Returned payment ($amount) cannot be greater than $maxAmount for return $refNum"
  }

  case class ReturnCcPaymentExceeded(refNum: String, amount: Long, maxAmount: Long) extends Failure {
    def description: String =
      s"Returned credit card payment ($amount) cannot be greater than $maxAmount for return $refNum"
  }

  case class ReturnCcPaymentCurrencyMismatch(refNum: String, expected: Currency, actual: List[Currency])
      extends Failure {
    def description: String =
      "Cannot have return for order with more than one currency. " +
        s"Expected $expected, but got: ${actual.mkString(", ")}"
  }

  case class ReturnCcPaymentViolation(refNum: String, issued: Long, allowed: Long) extends Failure {
    def description: String =
      s"Issued credit card payment ($issued) is different than $allowed for return $refNum"
  }
}
