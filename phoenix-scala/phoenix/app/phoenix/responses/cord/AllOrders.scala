package responses.cord

import java.time.Instant

import scala.concurrent.Future

import models.account.User
import models.cord.{CordPaymentState, Order}
import responses.ResponseItem

object AllOrders {
  type Response = Future[Seq[Root]]

  case class Root(referenceNumber: String,
                  orderState: Order.State,
                  email: Option[String] = None,
                  name: Option[String] = None,
                  // FIXME: why Option?
                  paymentState: Option[CordPaymentState.State] = None,
                  shippingState: Option[Order.State],
                  placedAt: Instant,
                  remorsePeriodEnd: Option[Instant] = None,
                  total: Int)
      extends ResponseItem

  def build(order: Order,
            customer: Option[User] = None,
            paymentState: Option[CordPaymentState.State] = None): Root = Root(
      referenceNumber = order.referenceNumber,
      orderState = order.state,
      name = customer.flatMap(_.name),
      email = customer.flatMap(_.email),
      paymentState = paymentState,
      shippingState = order.getShippingState,
      placedAt = order.placedAt,
      remorsePeriodEnd = order.getRemorsePeriodEnd,
      total = order.grandTotal
  )
}
