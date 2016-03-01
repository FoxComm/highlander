package responses.order

import java.time.Instant

import scala.concurrent.Future

import models.customer.Customer
import models.order.Order
import models.payment.creditcard.CreditCardCharge
import responses.ResponseItem
import utils.aliases._

object AllOrders {
  type Response = Future[Seq[Root]]

  final case class Root(
    referenceNumber: String,
    email: String,
    name: Option[String] = None,
    orderState: Order.State,
    paymentState: CreditCardCharge.State,
    shippingState: Option[Order.State] = None,
    placedAt: Option[Instant] = None,
    remorsePeriodEnd: Option[Instant] = None,
    total: Int) extends ResponseItem with OrderResponseBase

  def build(order: Order, customer: Customer, paymentState: CreditCardCharge.State)(implicit ec: EC): Root = Root(
      referenceNumber = order.referenceNumber,
      name = customer.name,
      email = customer.email,
      orderState = order.state,
      paymentState = paymentState,
      shippingState = order.getShippingState,
      placedAt = order.placedAt,
      remorsePeriodEnd = order.getRemorsePeriodEnd,
      total = order.grandTotal
  )
}
