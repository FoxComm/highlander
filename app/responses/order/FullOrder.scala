package responses.order

import java.time.Instant

import scala.concurrent.Future

import cats.implicits._
import models.customer.{Customer, Customers}
import models.location.Region
import models.objects._
import models.order._
import models.product.Mvp
import models.promotion._
import models.coupon._
import models.discount._
import models.objects.ObjectUtils.Child
import models.order.lineitems._
import models.payment.PaymentMethod
import models.payment.creditcard._
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit
import models.shipping.ShippingMethod
import models.{StoreAdmin, shipping}
import responses.CouponResponses.IlluminatedCouponResponse
import responses.PromotionResponses.IlluminatedPromotionResponse
import responses._
import services.orders.OrderQueries
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.aliases._

object FullOrder {
  type Response = Future[Root]
  type CcPayment = (OrderPayment, CreditCard, Region)

  case class Totals(subTotal: Int, taxes: Int, shipping: Int, adjustments: Int, total: Int) extends ResponseItem

  object Totals {
    def empty: Totals = Totals(0,0,0,0,0)
  }

  case class LineItems(
    skus: Seq[DisplayLineItem] = Seq.empty,
    giftCards: Seq[GiftCardResponse.Root] = Seq.empty
   ) extends ResponseItem

  case class LineItemAdjustment(
    adjustmentType: OrderLineItemAdjustment.AdjustmentType,
    substract: Int,
    lineItemId: Option[Int]
  ) extends ResponseItem

  object LineItemAdjustment {
    def build(model: OrderLineItemAdjustment) = LineItemAdjustment(adjustmentType = model.adjustmentType,
      substract = model.substract, lineItemId = model.lineItemId)
  }

  case class CouponPair(coupon: IlluminatedCouponResponse.Root, code: String) extends ResponseItem

  case class Root(
    id: Int,
    referenceNumber: String,
    orderState: Order.State,
    shippingState: Option[Order.State] = None,
    paymentState: Option[CreditCardCharge.State] = None,
    lineItems: LineItems,
    lineItemAdjustments: Seq[LineItemAdjustment] = Seq.empty,
    promotion: Option[IlluminatedPromotionResponse.Root] = None,
    coupon: Option[CouponPair] = None,
    fraudScore: Int,
    totals: Totals,
    customer: Option[CustomerResponse.Root] = None,
    shippingMethod: Option[ShippingMethods.Root] = None,
    shippingAddress: Option[Addresses.Root] = None,
    remorsePeriodEnd: Option[Instant] = None,
    paymentMethods: Seq[Payments] = Seq.empty,
    lockedBy: Option[StoreAdmin]) extends ResponseItem with OrderResponseBase

  case class DisplayLineItem(
    imagePath: String,
    referenceNumber: String = "",
    name: String = "donkey product",
    sku: String,
    price: Int = 33,
    quantity: Int = 1,
    totalPrice: Int = 33,
    state: OrderLineItem.State) extends ResponseItem

  sealed trait Payments

  object Payments {
    import PaymentMethod.{CreditCard, GiftCard, StoreCredit, Type}

    case class CreditCardPayment(id: Int, customerId: Int, holderName: String, lastFour: String, expMonth: Int,
      expYear: Int, brand: String, address: Addresses.Root, `type`: Type = CreditCard) extends Payments

    case class GiftCardPayment(code: String, amount: Int, currentBalance: Int, availableBalance: Int,
      createdAt: Instant, `type`: Type = GiftCard) extends Payments

    case class StoreCreditPayment(id: Int, amount: Int, currentBalance: Int, availableBalance: Int,
      createdAt: Instant, `type`: Type = StoreCredit) extends Payments
  }

  def refreshAndFullOrder(order: Order)(implicit ec: EC): DBIO[FullOrder.Root] =
    Orders.refresh(order).flatMap(fromOrder)

  def fromOrder(order: Order)(implicit ec: EC): DBIO[Root] = {
    fetchOrderDetails(order).map {
      case (customer, lineItems, shipMethod, shipAddress, ccPmt, gcPmts, scPmts, giftCards,
        totals, lockedBy, payState, promoDetails, lineItemAdjustments) ⇒
      build(
        order = order,
        customer = customer,
        lineItems = lineItems,
        giftCards = giftCards,
        shippingAddress = shipAddress.toOption,
        shippingMethod = shipMethod,
        ccPmt = ccPmt,
        gcPmts = gcPmts,
        scPmts = scPmts,
        lockedBy = lockedBy,
        paymentState = payState,
        totals = totals,
        promotion = promoDetails.map { case (promo, _) ⇒ promo },
        coupon = promoDetails.map { case (_, coupon) ⇒ coupon },
        lineItemAdjustments = lineItemAdjustments.map(LineItemAdjustment.build)
      )
    }
  }

  def build(order: Order, lineItems: Seq[OrderLineItemProductData] = Seq.empty,
    shippingMethod: Option[ShippingMethod] = None, customer: Option[Customer] = None,
    shippingAddress: Option[Addresses.Root] = None,
    ccPmt: Option[CcPayment] = None, gcPmts: Seq[(OrderPayment, GiftCard)] = Seq.empty,
    scPmts: Seq[(OrderPayment, StoreCredit)] = Seq.empty,
    giftCards: Seq[(GiftCard, OrderLineItemGiftCard)] = Seq.empty,
    totals: Option[Totals] = None,
    lockedBy: Option[StoreAdmin] = None,
    paymentState: Option[CreditCardCharge.State] = None,
    promotion: Option[IlluminatedPromotionResponse.Root] = None,
    coupon: Option[CouponPair] = None,
    lineItemAdjustments: Seq[LineItemAdjustment] = Seq.empty): Root = {

    val creditCardPmt = ccPmt.map { case (pmt, cc, region) ⇒
      val payment = Payments.CreditCardPayment(id = cc.id, customerId = cc.customerId, holderName = cc.holderName,
        lastFour = cc.lastFour, expMonth = cc.expMonth, expYear = cc.expYear, brand = cc.brand,
        address = Addresses.buildFromCreditCard(cc, region))
      Seq(payment)
    }.getOrElse(Seq.empty)

    val giftCardPmts = gcPmts.map { case (pmt, gc) ⇒
      Payments.GiftCardPayment(code = gc.code, amount = pmt.amount.getOrElse(0), currentBalance = gc.currentBalance,
        availableBalance = gc.availableBalance, createdAt = gc.createdAt)
    }

    val storeCreditPmts = scPmts.map { case (pmt, sc) ⇒
      Payments.StoreCreditPayment(id = sc.id, amount = pmt.amount.getOrElse(0), currentBalance = sc.currentBalance,
        availableBalance = sc.availableBalance, createdAt = sc.createdAt)
    }

    val paymentMethods: Seq[Payments] = creditCardPmt ++ giftCardPmts ++ storeCreditPmts

    val skuList = lineItems.map { 
      case data ⇒
        val price = Mvp.priceAsInt(data.skuForm, data.skuShadow)
        val name = Mvp.name(data.skuForm, data.skuShadow).getOrElse("")
        val noImage = "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/no_image.jpg"
        val image = Mvp.firstImage(data.skuForm, data.skuShadow).getOrElse(noImage)
 
        DisplayLineItem(imagePath = image, sku = data.sku.code, 
          referenceNumber = data.lineItem.referenceNumber, 
          state = data.lineItem.state, name = name, price = price, 
          totalPrice = price)
    }
    val gcList = giftCards.map { case (gc, li) ⇒ GiftCardResponse.build(gc) }

    Root(id = order.id,
      referenceNumber = order.referenceNumber,
      orderState = order.state,
      shippingState = order.getShippingState,
      paymentState = paymentState,
      lineItems = LineItems(skus = skuList, giftCards = gcList),
      lineItemAdjustments = lineItemAdjustments,
      promotion = promotion,
      coupon = coupon,
      fraudScore = order.fraudScore,
      customer = customer.map(responses.CustomerResponse.build(_)),
      shippingAddress = shippingAddress,
      totals = totals.getOrElse(Totals.empty),
      shippingMethod = shippingMethod.map(ShippingMethods.build(_)),
      remorsePeriodEnd = order.getRemorsePeriodEnd,
      paymentMethods = paymentMethods,
      lockedBy = none
    )
  }

  private def currentLock(order: Order): DBIO[Option[StoreAdmin]] = {
    if (order.isLocked) {
      (for {
        lock    ← OrderLockEvents.latestLockByOrder(order.id)
        admin   ← lock.storeAdmin
      } yield admin).one
    } else {
      DBIO.successful(none)
    }
  }

  private def totals(order: Order): Totals =
    Totals(subTotal = order.subTotal, shipping = order.shippingTotal, adjustments = order.adjustmentsTotal,
    taxes = order.taxesTotal, total = order.grandTotal)

  type PromoDetails = (IlluminatedPromotionResponse.Root, CouponPair)

  private def fetchPromoDetails(context: ObjectContext, orderPromo: Option[OrderPromotion])
    (implicit ec: EC): DBIO[Option[PromoDetails]] = orderPromo match {
      case Some(op) ⇒ fetchCouponDetails(context, op.couponCodeId) // TBD: Handle auto-apply promos here
      case _        ⇒ DBIO.successful(None)
  }

  private def fetchCouponDetails(context: ObjectContext, couponCodeId: Option[Int])(implicit ec: EC):
    DBIO[Option[PromoDetails]] = couponCodeId match {
      case Some(codeId) ⇒ for {
        // Fetch coupon
        couponCode     ← CouponCodes.findById(codeId).extract.one.safeGet
        coupon         ← Coupons.filterByContextAndFormId(context.id, codeId).one.safeGet
        couponForm     ← ObjectForms.findById(coupon.formId).extract.one.safeGet
        couponShadow   ← ObjectShadows.findById(coupon.shadowId).extract.one.safeGet
        // Fetch promo
        promotion      ← Promotions.filterByContextAndFormId(context.id, coupon.promotionId).one.safeGet
        promoForm      ← ObjectForms.findById(promotion.formId).extract.one.safeGet
        promoShadow    ← ObjectShadows.findById(promotion.shadowId).extract.one.safeGet
        // Fetch discounts
        links          ← ObjectLinks.filter(_.leftId === promotion.shadowId).result
        shadows        ← ObjectShadows.filter(_.id.inSet(links.map(_.rightId))).result
        forms          ← ObjectForms.filter(_.id.inSet(shadows.map(_.formId))).result
        discounts      = forms.sortBy(_.id).zip(shadows.sortBy(_.formId)).map { case (f, s) ⇒ Child(f, s) }
        // Illuminate
        illumDiscounts = discounts.map(d ⇒ IlluminatedDiscount.illuminate(form = d.form, shadow = d.shadow))
        illumPromo     = IlluminatedPromotion.illuminate(context, promotion, promoForm, promoShadow)
        illumCoupon    = IlluminatedCoupon.illuminate(context, coupon, couponForm, couponShadow)
        // Build responses
        respPromo      = IlluminatedPromotionResponse.build(illumPromo, illumDiscounts)
        respCoupon     = IlluminatedCouponResponse.build(illumCoupon)
        respCouponPair = CouponPair(coupon = respCoupon, code = couponCode.code)
      } yield (respPromo, respCouponPair).some
      case _ ⇒ DBIO.successful(None)
  }

  private def fetchOrderDetails(order: Order)(implicit ec: EC) = {
    val ccPaymentQ = for {
      payment     ← OrderPayments.findAllByOrderId(order.id)
      creditCard  ← CreditCards.filter(_.id === payment.paymentMethodId)
      region      ← creditCard.region
    } yield (payment, creditCard, region)

    for {
      context      ← ObjectContexts.findById(order.contextId).extract.one.safeGet
      customer     ← Customers.findById(order.customerId).extract.one
      lineItemTup  ← OrderLineItemSkus.findLineItemsByOrder(order).result
      lineItems    = lineItemTup.map {
        case (sku, skuForm, skuShadow, lineItem) ⇒ 
          OrderLineItemProductData(sku, skuForm, skuShadow, lineItem)
      }
      giftCards    ← OrderLineItemGiftCards.findLineItemsByOrder(order).result
      shipMethod   ← shipping.ShippingMethods.forOrder(order).one
      shipAddress  ← Addresses.forOrderId(order.id)
      payments     ← ccPaymentQ.one
      gcPayments   ← OrderPayments.findAllGiftCardsByOrderId(order.id).result
      scPayments   ← OrderPayments.findAllStoreCreditsByOrderId(order.id).result
      lockedBy     ← currentLock(order)
      payState     ← OrderQueries.getPaymentState(order.id)
      // Promotion stuff
      orderPromo   ← OrderPromotions.filterByOrderId(order.id).one
      promoDetails ← fetchPromoDetails(context, orderPromo)
      lineItemAdj  ← OrderLineItemAdjustments.findByOrderId(order.id).result
    } yield (
      customer, 
      lineItems,
      shipMethod, 
      shipAddress, 
      payments, 
      gcPayments, 
      scPayments,
      giftCards, 
      totals(order).some,
      lockedBy, 
      payState.some,
      promoDetails,
      lineItemAdj)
  }
}
