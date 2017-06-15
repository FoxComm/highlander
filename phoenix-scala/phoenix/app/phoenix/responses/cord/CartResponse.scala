package phoenix.responses.cord

import cats.implicits._
import core.db._
import phoenix.models.account._
import phoenix.models.cord.OrderPayments.scope._
import phoenix.models.cord._
import phoenix.models.cord.lineitems.CartLineItems
import phoenix.models.customer.{CustomerData, CustomersData}
import phoenix.responses.PromotionResponses.PromotionResponse
import phoenix.responses._
import phoenix.responses.cord.base._
import phoenix.responses.users.CustomerResponse
import phoenix.services.carts.CartQueries
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

case class CartResponse(referenceNumber: String,
                        paymentState: CordPaymentState.State,
                        lineItems: CordResponseLineItems,
                        lineItemAdjustments: Seq[CordResponseLineItemAdjustment] = Seq.empty,
                        promotion: Option[PromotionResponse.Root] = None,
                        coupon: Option[CordResponseCouponPair] = None,
                        totals: CartResponseTotals,
                        customer: Option[CustomerResponse] = None,
                        shippingMethod: Option[ShippingMethodsResponse.Root] = None,
                        shippingAddress: Option[AddressResponse] = None,
                        paymentMethods: Seq[CordResponsePayments] = Seq.empty)
    extends ResponseItem

object CartResponse {

  def buildRefreshed(cart: Cart)(implicit db: DB, ec: EC, ctx: OC): DbResultT[CartResponse] =
    Carts.refresh(cart).dbresult.flatMap(c ⇒ fromCart(c, grouped = true))

  def fromCart(cart: Cart, grouped: Boolean, isGuest: Boolean = false)(implicit db: DB,
                                                                       ec: EC,
                                                                       ctx: OC): DbResultT[CartResponse] =
    for {
      lineItemAdj    ← * <~ CordResponseLineItemAdjustments.fetch(cart.refNum)
      lineItemsSku   ← * <~ CartLineItems.byCordRef(cart.refNum).result
      lineItems      ← * <~ CordResponseLineItems.fetchCart(cart.refNum, lineItemAdj, grouped)
      promo          ← * <~ CordResponsePromotions.fetch(cart.refNum)
      customer       ← * <~ Users.findOneByAccountId(cart.accountId)
      customerData   ← * <~ CustomersData.findOneByAccountId(cart.accountId)
      shippingMethod ← * <~ CordResponseShipping.shippingMethod(cart.refNum)
      shippingAddress ← * <~ CordResponseShipping
                         .shippingAddress(cart.refNum)
                         .fold(_ ⇒ None, good ⇒ good.some)
      paymentMethods ← * <~ (if (isGuest) DBIO.successful(Seq())
                             else CordResponsePayments.fetchAll(cart.refNum))
      paymentState ← * <~ CartQueries.getCordPaymentState(cart.refNum)
      coveredByInStoreMethods ← * <~ OrderPayments
                                 .findAllByCordRef(cart.refNum)
                                 .inStoreMethods
                                 .map(_.amount.getOrElse(0L))
                                 .sum
                                 .getOrElse(0L)
                                 .result
    } yield
      CartResponse(
        referenceNumber = cart.refNum,
        lineItems = lineItems,
        lineItemAdjustments = lineItemAdj,
        promotion = promo.map { case (promotion, _) ⇒ promotion },
        coupon = promo.flatMap { case (_, coupon)   ⇒ coupon },
        totals = CartResponseTotals.build(cart, coveredByInStoreMethods = coveredByInStoreMethods),
        customer = for {
          c  ← customer
          cu ← customerData
        } yield CustomerResponse.build(c, cu),
        shippingMethod = shippingMethod,
        shippingAddress = shippingAddress,
        paymentMethods = paymentMethods,
        paymentState = paymentState
      )

  def buildEmpty(cart: Cart,
                 customer: Option[User] = None,
                 customerData: Option[CustomerData] = None): CartResponse =
    CartResponse(
      referenceNumber = cart.refNum,
      lineItems = CordResponseLineItems(),
      customer = for {
        c  ← customer
        cu ← customerData
      } yield CustomerResponse.build(c, cu),
      totals = CartResponseTotals.empty,
      paymentState = CordPaymentState.Cart
    )

}
