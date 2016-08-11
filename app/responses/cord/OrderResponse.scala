package responses.cord

import java.time.Instant

import failures.ShippingMethodFailures.ShippingMethodNotFoundInOrder
import models.cord._
import models.customer.Customers
import models.objects._
import models.payment.creditcard._
import responses.PromotionResponses.IlluminatedPromotionResponse
import responses._
import responses.cord.base._
import services.orders.OrderQueries
import utils.aliases._
import utils.db._

case class OrderResponse(referenceNumber: String,
                         paymentState: CreditCardCharge.State,
                         lineItems: CordResponseLineItems,
                         lineItemAdjustments: Seq[CordResponseLineItemAdjustment] = Seq.empty,
                         promotion: Option[IlluminatedPromotionResponse.Root] = None,
                         coupon: Option[CordResponseCouponPair] = None,
                         totals: CordResponseTotals,
                         customer: Option[CustomerResponse.Root] = None,
                         shippingMethod: ShippingMethodsResponse.Root,
                         shippingAddress: AddressResponse,
                         paymentMethods: Seq[_ <: CordResponsePayments] = Seq.empty,
                         // Order-specific
                         orderState: Order.State,
                         shippingState: Option[Order.State] = None,
                         fraudScore: Int,
                         remorsePeriodEnd: Option[Instant] = None)
    extends ResponseItem

object OrderResponse {

  def fromOrder(order: Order)(implicit db: DB, ec: EC): DbResultT[OrderResponse] =
    for {
      context     ← * <~ ObjectContexts.mustFindById400(order.contextId)
      payState    ← * <~ OrderQueries.getPaymentState(order.refNum)
      lineItemAdj ← * <~ CordResponseLineItemAdjustments.fetch(order.refNum)
      lineItems   ← * <~ CordResponseLineItems.fetch(order.refNum, lineItemAdj)
      promo       ← * <~ CordResponsePromotions.fetch(order.refNum)(db, ec, context)
      customer    ← * <~ Customers.findOneById(order.customerId)
      shippingMethod ← * <~ CordResponseShipping
                        .shippingMethod(order.refNum)
                        .mustFindOr(ShippingMethodNotFoundInOrder(order.refNum))
      shippingAddress ← * <~ CordResponseShipping.shippingAddress(order.refNum)
      paymentMethods  ← * <~ CordResponsePayments.fetchAll(order.refNum)
    } yield
      OrderResponse(
          referenceNumber = order.refNum,
          paymentState = payState,
          lineItems = lineItems,
          lineItemAdjustments = lineItemAdj,
          promotion = promo.map { case (promotion, _) ⇒ promotion },
          coupon = promo.map { case (_, coupon)       ⇒ coupon },
          totals = CordResponseTotals.build(order),
          customer = customer.map(CustomerResponse.build(_)),
          shippingMethod = shippingMethod,
          shippingAddress = shippingAddress,
          paymentMethods = paymentMethods,
          orderState = order.state,
          shippingState = order.getShippingState,
          fraudScore = order.fraudScore,
          remorsePeriodEnd = order.remorsePeriodEnd
      )

}
