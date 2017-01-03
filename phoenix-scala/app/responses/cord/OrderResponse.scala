package responses.cord

import java.time.Instant

import failures.ShippingMethodFailures.ShippingMethodNotFoundInOrder
import models.account._
import models.cord._
import models.customer.CustomersData
import models.objects._
import responses.PromotionResponses.PromotionResponse
import responses._
import responses.cord.base._
import services.orders.OrderQueries
import utils.aliases._
import utils.db._

case class OrderResponse(referenceNumber: String,
                         paymentState: CordPaymentState.State,
                         lineItems: CordResponseLineItems,
                         lineItemAdjustments: Seq[CordResponseLineItemAdjustment] = Seq.empty,
                         promotion: Option[PromotionResponse.Root] = None,
                         coupon: Option[CordResponseCouponPair] = None,
                         totals: OrderResponseTotals,
                         customer: Option[CustomerResponse.Root] = None,
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
      paymentMethods: Seq[_ <: CordResponsePayments]): Option[CordResponseCreditCardPayment] = {
    paymentMethods.collectFirst {
      case ccPayment: CordResponseCreditCardPayment ⇒ ccPayment
    }
  }

  def fromOrder(order: Order, grouped: Boolean)(implicit db: DB,
                                                ec: EC): DbResultT[OrderResponse] =
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
          coupon = promo.map { case (_, coupon)       ⇒ coupon },
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
