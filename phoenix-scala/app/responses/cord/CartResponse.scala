package responses.cord

import cats.implicits._
import models.StoreAdmin
import models.cord._
import models.cord.lineitems.CartLineItems
import models.account._
import models.payment.creditcard._
import responses.PromotionResponses.IlluminatedPromotionResponse
import responses._
import responses.cord.base._
import services.carts.CartQueries
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

case class CartResponse(referenceNumber: String,
                        paymentState: CreditCardCharge.State,
                        lineItems: CordResponseLineItems,
                        lineItemAdjustments: Seq[CordResponseLineItemAdjustment] = Seq.empty,
                        promotion: Option[IlluminatedPromotionResponse.Root] = None,
                        coupon: Option[CordResponseCouponPair] = None,
                        totals: CordResponseTotals,
                        customer: Option[CustomerResponse.Root] = None,
                        shippingMethod: Option[ShippingMethodsResponse.Root] = None,
                        shippingAddress: Option[AddressResponse] = None,
                        paymentMethods: Seq[CordResponsePayments] = Seq.empty,
                        // Cart-specific
                        lockedBy: Option[StoreAdmin] = None)
    extends ResponseItem

object CartResponse {

  def buildRefreshed(cart: Cart)(implicit db: DB, ec: EC, ctx: OC): DbResultT[CartResponse] =
    Carts.refresh(cart).toXor.flatMap(fromCart)

  def fromCart(cart: Cart)(implicit db: DB, ec: EC, ctx: OC): DbResultT[CartResponse] =
    for {
      lineItemAdj    ← * <~ CordResponseLineItemAdjustments.fetch(cart.refNum)
      lineItemsSku   ← * <~ CartLineItems.byCordRef(cart.refNum).result
      lineItems      ← * <~ CordResponseLineItems.fetchCart(cart.refNum, lineItemAdj)
      promo          ← * <~ CordResponsePromotions.fetch(cart.refNum)
      customer       ← * <~ Users.findOneById(cart.accountId)
      shippingMethod ← * <~ CordResponseShipping.shippingMethod(cart.refNum)
      shippingAddress ← * <~ CordResponseShipping
                         .shippingAddress(cart.refNum)
                         .fold(_ ⇒ None, good ⇒ good.some)
      paymentMethods ← * <~ CordResponsePayments.fetchAll(cart.refNum)
      paymentState   ← * <~ CartQueries.getPaymentState(cart.refNum)
      lockedBy       ← * <~ currentLock(cart)
    } yield
      CartResponse(
          referenceNumber = cart.refNum,
          lineItems = lineItems,
          lineItemAdjustments = lineItemAdj,
          promotion = promo.map { case (promotion, _) ⇒ promotion },
          coupon = promo.map { case (_, coupon)       ⇒ coupon },
          totals = CordResponseTotals.build(cart),
          customer = customer.map(CustomerResponse.build(_)),
          shippingMethod = shippingMethod,
          shippingAddress = shippingAddress,
          paymentMethods = paymentMethods,
          paymentState = paymentState,
          lockedBy = lockedBy
      )

  def buildEmpty(cart: Cart, customer: Option[Customer]): CartResponse =
    CartResponse(
        referenceNumber = cart.refNum,
        lineItems = CordResponseLineItems(),
        customer = customer.map(CustomerResponse.build(_)),
        totals = CordResponseTotals.empty,
        paymentState = CreditCardCharge.Cart
    )

  private def currentLock(cart: Cart): DBIO[Option[StoreAdmin]] =
    if (cart.isLocked) (for {
      lock  ← CartLockEvents.latestLockByCartRef(cart.refNum)
      admin ← lock.storeAdmin
    } yield admin).one
    else DBIO.successful(none)
}
