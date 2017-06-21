package phoenix.responses.cord

import java.time.Instant

import core.db._
import objectframework.models._
import phoenix.failures.ShippingMethodFailures.ShippingMethodNotFoundInOrder
import phoenix.models.account._
import phoenix.models.cord._
import phoenix.models.customer.CustomersData
import phoenix.responses.PromotionResponses.PromotionResponse
import phoenix.responses._
import phoenix.responses.cord.base._
import phoenix.responses.users.CustomerResponse
import phoenix.services.orders.OrderQueries

case class OrderResponse(referenceNumber: String,
                         paymentState: CordPaymentState.State,
                         lineItems: CordResponseLineItems,
                         lineItemAdjustments: Seq[CordResponseLineItemAdjustment] = Seq.empty,
                         promotion: Option[PromotionResponse.Root] = None,
                         coupon: Option[CordResponseCouponPair] = None,
                         totals: OrderResponseTotals,
                         customer: Option[CustomerResponse] = None,
                         shippingMethod: ShippingMethodsResponse.Root,
                         shippingAddress: AddressResponse,
                         billingAddress: Option[AddressResponse] = None,
                         billingCreditCardInfo: Option[CordResponseCreditCardPayment] = None,
                         paymentMethods: Seq[_ <: CordResponsePayments] = Seq.empty,
                         // Order-specific
                         orderState: Order.State,
                         shippingState: Option[Order.State] = None,
                         fraudScore: Int,
                         remorsePeriodEnd: Option[Instant] = None,
                         placedAt: Instant)
    extends ResponseItem

object OrderResponse {

  private def getCreditCardResponse(
      paymentMethods: Seq[_ <: CordResponsePayments]): Option[CordResponseCreditCardPayment] =
    paymentMethods.collectFirst {
      case ccPayment: CordResponseCreditCardPayment ⇒ ccPayment
    }

  def fromOrder(order: Order, grouped: Boolean)(implicit db: DB, ec: EC): DbResultT[OrderResponse] =
    for {
      context      ← * <~ ObjectContexts.mustFindById400(order.contextId)
      payState     ← * <~ OrderQueries.getCordPaymentState(order.refNum)
      lineItemAdj  ← * <~ CordResponseLineItemAdjustments.fetch(order.refNum)
      lineItems    ← * <~ CordResponseLineItems.fetch(order.refNum, lineItemAdj, grouped)
      promo        ← * <~ CordResponsePromotions.fetch(order.refNum)(db, ec, context)
      customer     ← * <~ Users.findOneByAccountId(order.accountId)
      customerData ← * <~ CustomersData.findOneByAccountId(order.accountId)
      shippingMethod ← * <~ CordResponseShipping
                        .shippingMethod(order.refNum)
                        .mustFindOr(ShippingMethodNotFoundInOrder(order.refNum))
      shippingAddress ← * <~ CordResponseShipping.shippingAddress(order.refNum)
      paymentMethods  ← * <~ CordResponsePayments.fetchAll(order.refNum)
      ccResponse = getCreditCardResponse(paymentMethods)
    } yield
      OrderResponse(
        referenceNumber = order.refNum,
        paymentState = payState,
        lineItems = lineItems,
        lineItemAdjustments = lineItemAdj,
        promotion = promo.map { case (promotion, _) ⇒ promotion },
        coupon = promo.flatMap { case (_, coupon)   ⇒ coupon },
        totals = OrderResponseTotals.build(order),
        customer = for {
          c  ← customer
          cu ← customerData
        } yield CustomerResponse.build(c, cu),
        shippingMethod = shippingMethod,
        shippingAddress = shippingAddress,
        billingCreditCardInfo = ccResponse,
        billingAddress = ccResponse.map(_.address),
        paymentMethods = paymentMethods,
        orderState = order.state,
        shippingState = order.getShippingState,
        fraudScore = order.fraudScore,
        remorsePeriodEnd = order.remorsePeriodEnd,
        placedAt = order.placedAt
      )

}
