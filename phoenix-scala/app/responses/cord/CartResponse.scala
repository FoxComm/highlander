package responses.cord

import cats.implicits._
import models.account.User
import models.cord._
import models.cord.lineitems.CartLineItems
import models.customer.{CustomersData, CustomerData}
import models.account._
import models.payment.creditcard._
import responses.PromotionResponses.PromotionResponse
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
                        promotion: Option[PromotionResponse.Root] = None,
                        coupon: Option[CordResponseCouponPair] = None,
                        totals: CordResponseTotals,
                        customer: Option[CustomerResponse.Root] = None,
                        shippingMethod: Option[ShippingMethodsResponse.Root] = None,
                        shippingAddress: Option[AddressResponse] = None,
                        paymentMethods: Seq[CordResponsePayments] = Seq.empty,
                        // Cart-specific
                        lockedBy: Option[User] = None)
    extends ResponseItem

object CartResponse {

  def buildRefreshed(cart: Cart)(implicit db: DB, ec: EC, ctx: OC): DbResultT[CartResponse] =
    Carts.refresh(cart).dbresult.flatMap(c ⇒ fromCart(c, grouped = true))

  def fromCart(cart: Cart, grouped: Boolean, isGuest: Boolean = false)(
      implicit db: DB,
      ec: EC,
      ctx: OC): DbResultT[CartResponse] =
    for {
      lineItemAdj    ← * <~ CordResponseLineItemAdjustments.fetch(cart.refNum)
      lineItems      ← * <~ CordResponseLineItems.fetchCart(cart.refNum, lineItemAdj, grouped)
      promo          ← * <~ CordResponsePromotions.fetch(cart.refNum)
      customer       ← * <~ Users.findOneByAccountId(cart.accountId)
      customerData   ← * <~ CustomersData.findOneByAccountId(cart.accountId)
      shippingMethod ← * <~ CordResponseShipping.shippingMethod(cart.refNum)
      shippingAddress ← * <~ CordResponseShipping
                         .shippingAddress(cart.refNum)
                         .fold(_ ⇒ None, good ⇒ good.some)
      paymentMethods ← * <~ {
                        if (isGuest) DBIO.successful(Seq())
                        else CordResponsePayments.fetchAll(cart.refNum)
                      }
      paymentState ← * <~ CartQueries.getPaymentState(cart.refNum)
      lockedBy     ← * <~ currentLock(cart)
    } yield
      CartResponse(
          referenceNumber = cart.refNum,
          lineItems = lineItems,
          lineItemAdjustments = lineItemAdj,
          promotion = promo.map { case (promotion, _) ⇒ promotion },
          coupon = promo.map { case (_, coupon)       ⇒ coupon },
          totals = CordResponseTotals.build(cart),
          customer = for {
            c  ← customer
            cu ← customerData
          } yield CustomerResponse.build(c, cu),
          shippingMethod = shippingMethod,
          shippingAddress = shippingAddress,
          paymentMethods = paymentMethods,
          paymentState = paymentState,
          lockedBy = lockedBy
      )

  def buildEmpty(cart: Cart,
                 customer: Option[User] = None,
                 customerData: Option[CustomerData] = None): CartResponse = {
    CartResponse(
        referenceNumber = cart.refNum,
        lineItems = CordResponseLineItems(),
        customer = for {
          c  ← customer
          cu ← customerData
        } yield CustomerResponse.build(c, cu),
        totals = CordResponseTotals.empty,
        paymentState = CreditCardCharge.Cart
    )
  }

  private def currentLock(cart: Cart): DBIO[Option[User]] =
    if (cart.isLocked) (for {
      lock  ← CartLockEvents.latestLockByCartRef(cart.refNum)
      admin ← lock.storeAdmin
    } yield admin).one
    else DBIO.successful(none)
}
