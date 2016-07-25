package responses.order

import java.time.Instant

import scala.concurrent.Future

import cats.implicits._
import failures.CouponFailures._
import failures.PromotionFailures._
import models.cord._
import models.cord.lineitems._
import models.coupon._
import models.customer.{Customer, Customers}
import models.discount._
import models.location.Region
import models.objects._
import models.payment.PaymentMethod
import models.payment.creditcard._
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit
import models.product.Mvp
import models.promotion.Promotions.scope._
import models.promotion._
import models.shipping.ShippingMethod
import models.{StoreAdmin, shipping}
import responses.CouponResponses.IlluminatedCouponResponse
import responses.PromotionResponses.IlluminatedPromotionResponse
import responses._
import services.orders.OrderQueries
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object FullOrder {
  type Response  = Future[Root]
  type CcPayment = (OrderPayment, CreditCard, Region)

  case class Totals(subTotal: Int, taxes: Int, shipping: Int, adjustments: Int, total: Int)
      extends ResponseItem

  object Totals {
    def empty: Totals = Totals(0, 0, 0, 0, 0)
  }

  case class LineItems(
      skus: Seq[DisplayLineItem] = Seq.empty,
      giftCards: Seq[GiftCardResponse.Root] = Seq.empty
  ) extends ResponseItem

  case class LineItemAdjustment(
      adjustmentType: OrderLineItemAdjustment.AdjustmentType,
      substract: Int,
      lineItemRefNum: Option[String]
  ) extends ResponseItem

  object LineItemAdjustment {
    def build(model: OrderLineItemAdjustment) =
      LineItemAdjustment(adjustmentType = model.adjustmentType,
                         substract = model.substract,
                         lineItemRefNum = model.lineItemRefNum)
  }

  case class CouponPair(coupon: IlluminatedCouponResponse.Root, code: String) extends ResponseItem

  case class Root(referenceNumber: String,
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
                  paymentMethods: Seq[Payments] = Seq.empty)
      extends ResponseItem
      with OrderResponseBase

  case class DisplayLineItem(imagePath: String,
                             referenceNumber: String,
                             name: String,
                             sku: String,
                             price: Int,
                             quantity: Int = 1,
                             totalPrice: Int,
                             productFormId: Int,
                             state: OrderLineItem.State)
      extends ResponseItem

  sealed trait Payments

  object Payments {
    import PaymentMethod.{CreditCard, GiftCard, StoreCredit, Type}

    case class CreditCardPayment(id: Int,
                                 customerId: Int,
                                 holderName: String,
                                 lastFour: String,
                                 expMonth: Int,
                                 expYear: Int,
                                 brand: String,
                                 address: Addresses.Root,
                                 `type`: Type = CreditCard)
        extends Payments

    case class GiftCardPayment(code: String,
                               amount: Int,
                               currentBalance: Int,
                               availableBalance: Int,
                               createdAt: Instant,
                               `type`: Type = GiftCard)
        extends Payments

    case class StoreCreditPayment(id: Int,
                                  amount: Int,
                                  currentBalance: Int,
                                  availableBalance: Int,
                                  createdAt: Instant,
                                  `type`: Type = StoreCredit)
        extends Payments
  }

  def refreshAndFullOrder(order: Order)(implicit db: DB, ec: EC): DbResultT[FullOrder.Root] =
    Orders.refresh(order).toXor.flatMap(fromOrder)

  def fromOrder(order: Order)(implicit db: DB, ec: EC): DbResultT[Root] = {
    val ccPaymentQ = for {
      payment    ← OrderPayments.findAllByOrderRef(order.refNum)
      creditCard ← CreditCards.filter(_.id === payment.paymentMethodId)
      region     ← creditCard.region
    } yield (payment, creditCard, region)

    for {
      context     ← * <~ ObjectContexts.mustFindById400(order.contextId)
      customer    ← * <~ Customers.findOneById(order.customerId)
      lineItemTup ← * <~ OrderLineItemSkus.findLineItemsByCordRef(order.refNum).result
      lineItems = lineItemTup.map {
        case (sku, skuForm, skuShadow, productShadow, lineItem) ⇒
          OrderLineItemProductData(sku, skuForm, skuShadow, productShadow, lineItem)
      }
      giftCards  ← * <~ OrderLineItemGiftCards.findLineItemsByCordRef(order.refNum).result
      shipMethod ← * <~ shipping.ShippingMethods.forCordRef(order.refNum).one
      shipAddress ← * <~ Addresses
                     .forOrderRef(order.refNum)
                     .fold(_ ⇒ Option.empty[Addresses.Root], address ⇒ address.some)
      payments     ← * <~ ccPaymentQ.one
      gcPayments   ← * <~ OrderPayments.findAllGiftCardsByOrderRef(order.refNum).result
      scPayments   ← * <~ OrderPayments.findAllStoreCreditsByOrderRef(order.refNum).result
      payState     ← * <~ OrderQueries.getPaymentState(order.refNum)
      orderPromo   ← * <~ OrderPromotions.filterByOrderRef(order.refNum).one
      promoDetails ← * <~ fetchPromoDetails(context, orderPromo)
      lineItemAdj  ← * <~ OrderLineItemAdjustments.findByOrderRef(order.refNum).result
    } yield
      build(
          order = order,
          customer = customer,
          lineItems = lineItems,
          giftCards = giftCards,
          shippingAddress = shipAddress,
          shippingMethod = shipMethod,
          ccPmt = payments,
          gcPmts = gcPayments,
          scPmts = scPayments,
          paymentState = payState.some,
          totals = totals(order).some,
          promotion = promoDetails.map { case (promo, _) ⇒ promo },
          coupon = promoDetails.map { case (_, coupon)   ⇒ coupon },
          lineItemAdjustments = lineItemAdj.map(LineItemAdjustment.build)
      )
  }

  def build(order: Order,
            lineItems: Seq[OrderLineItemProductData] = Seq.empty,
            shippingMethod: Option[ShippingMethod] = None,
            customer: Option[Customer] = None,
            shippingAddress: Option[Addresses.Root] = None,
            ccPmt: Option[CcPayment] = None,
            gcPmts: Seq[(OrderPayment, GiftCard)] = Seq.empty,
            scPmts: Seq[(OrderPayment, StoreCredit)] = Seq.empty,
            giftCards: Seq[(GiftCard, OrderLineItemGiftCard)] = Seq.empty,
            totals: Option[Totals] = None,
            lockedBy: Option[StoreAdmin] = None,
            paymentState: Option[CreditCardCharge.State] = None,
            promotion: Option[IlluminatedPromotionResponse.Root] = None,
            coupon: Option[CouponPair] = None,
            lineItemAdjustments: Seq[LineItemAdjustment] = Seq.empty): Root = {

    val creditCardPmt = ccPmt.map {
      case (pmt, cc, region) ⇒
        val payment = Payments.CreditCardPayment(id = cc.id,
                                                 customerId = cc.customerId,
                                                 holderName = cc.holderName,
                                                 lastFour = cc.lastFour,
                                                 expMonth = cc.expMonth,
                                                 expYear = cc.expYear,
                                                 brand = cc.brand,
                                                 address =
                                                   Addresses.buildFromCreditCard(cc, region))
        Seq(payment)
    }.getOrElse(Seq.empty)

    val giftCardPmts = gcPmts.map {
      case (pmt, gc) ⇒
        Payments.GiftCardPayment(code = gc.code,
                                 amount = pmt.amount.getOrElse(0),
                                 currentBalance = gc.currentBalance,
                                 availableBalance = gc.availableBalance,
                                 createdAt = gc.createdAt)
    }

    val storeCreditPmts = scPmts.map {
      case (pmt, sc) ⇒
        Payments.StoreCreditPayment(id = sc.id,
                                    amount = pmt.amount.getOrElse(0),
                                    currentBalance = sc.currentBalance,
                                    availableBalance = sc.availableBalance,
                                    createdAt = sc.createdAt)
    }

    val paymentMethods: Seq[Payments] = creditCardPmt ++ giftCardPmts ++ storeCreditPmts

    val skuList = lineItems.map { data ⇒
      val price = Mvp.priceAsInt(data.skuForm, data.skuShadow)
      val name  = Mvp.name(data.skuForm, data.skuShadow).getOrElse("")
      val noImage =
        "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/no_image.jpg"
      val image = Mvp.firstImage(data.skuForm, data.skuShadow).getOrElse(noImage)

      DisplayLineItem(imagePath = image,
                      sku = data.sku.code,
                      referenceNumber = data.lineItem.referenceNumber,
                      state = data.lineItem.state,
                      name = name,
                      price = price,
                      productFormId = data.product.formId,
                      totalPrice = price)
    }

    val gcList = giftCards.map { case (gc, li) ⇒ GiftCardResponse.build(gc) }

    Root(referenceNumber = order.referenceNumber,
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
         paymentMethods = paymentMethods)
  }

  private def totals(order: Order): Totals =
    Totals(subTotal = order.subTotal,
           shipping = order.shippingTotal,
           adjustments = order.adjustmentsTotal,
           taxes = order.taxesTotal,
           total = order.grandTotal)

  type PromoDetails = Option[(IlluminatedPromotionResponse.Root, CouponPair)]

  private def fetchPromoDetails(context: ObjectContext, orderPromo: Option[OrderPromotion])(
      implicit db: DB,
      ec: EC): DBIO[PromoDetails] = orderPromo match {
    case Some(op) ⇒
      fetchCouponDetails(context, op.couponCodeId) // TBD: Handle auto-apply promos here later
    case _ ⇒ DBIO.successful(None)
  }

  private def fetchCouponDetails(context: ObjectContext, couponCodeId: Option[Int])(
      implicit db: DB,
      ec: EC): DBIO[PromoDetails] = couponCodeId match {
    case Some(codeId) ⇒
      fetchCouponInner(context, codeId).fold(_ ⇒ None, option ⇒ option)
    case _ ⇒ DBIO.successful(None)
  }

  // TBD: Get discounts from cached field in `OrderPromotion` model
  private def fetchCouponInner(context: ObjectContext, couponCodeId: Int)(implicit db: DB,
                                                                          ec: EC) =
    for {
      // Coupon
      couponCode ← * <~ CouponCodes
                    .findOneById(couponCodeId)
                    .mustFindOr(CouponCodeNotFound(couponCodeId))
      coupon ← * <~ Coupons
                .filterByContextAndFormId(context.id, couponCode.couponFormId)
                .mustFindOneOr(CouponWithCodeCannotBeFound(couponCode.code))
      couponForm   ← * <~ ObjectForms.mustFindById404(coupon.formId)
      couponShadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
      // Promotion
      promotion ← * <~ Promotions
                   .filterByContextAndFormId(context.id, coupon.promotionId)
                   .requiresCoupon
                   .mustFindOneOr(PromotionNotFound(coupon.promotionId))
      promoForm   ← * <~ ObjectForms.mustFindById404(promotion.formId)
      promoShadow ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)

      discounts ← * <~ PromotionDiscountLinks.queryRightByLeft(promotion)
      // Illuminate
      theCoupon = IlluminatedCoupon.illuminate(context, coupon, couponForm, couponShadow)
      theDiscounts = discounts.map(discount ⇒
            IlluminatedDiscount.illuminate(context.some, discount.form, discount.shadow))
      thePromotion = IlluminatedPromotion.illuminate(context, promotion, promoForm, promoShadow)
      // Responses
      respPromo      = IlluminatedPromotionResponse.build(thePromotion, theDiscounts)
      respCoupon     = IlluminatedCouponResponse.build(theCoupon)
      respCouponPair = CouponPair(coupon = respCoupon, code = couponCode.code)
    } yield (respPromo, respCouponPair).some

}
