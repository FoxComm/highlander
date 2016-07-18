package responses.order

import java.time.Instant

import scala.concurrent.Future

import models.cord.Order
import models.customer.Customer
import models.payment.creditcard.CreditCardCharge
import responses.ResponseItem

object AllOrders {
  type Response = Future[Seq[Root]]

  case class Root(referenceNumber: String,
                  orderState: Order.State,
                  email: Option[String] = None,
                  name: Option[String] = None,
                  paymentState: Option[CreditCardCharge.State] = None,
                  shippingState: Option[Order.State] = None,
                  placedAt: Instant,
                  remorsePeriodEnd: Option[Instant] = None,
                  total: Int)
      extends ResponseItem
      with OrderResponseBase

  def build(order: Order,
            customer: Option[Customer] = None,
            paymentState: Option[CreditCardCharge.State] = None): Root = Root(
      referenceNumber = order.referenceNumber,
      orderState = order.state,
      name = customer.flatMap(_.name),
      email = customer.map(_.email),
      paymentState = paymentState,
      shippingState = order.getShippingState,
      placedAt = order.placedAt,
      remorsePeriodEnd = order.getRemorsePeriodEnd,
      total = order.grandTotal
  )
}
