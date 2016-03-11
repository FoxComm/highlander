package responses.order

import java.time.Instant
import scala.concurrent.Future

import cats.implicits._
import models.customer.{Customers, Customer}
import models.product.Mvp
import models.location.Region
import models.order._
import models.order.lineitems._
import models.payment.PaymentMethod
import models.payment.creditcard._
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit
import models.shipping.ShippingMethod
import models.{StoreAdmin, shipping}
import responses._
import services.orders.OrderQueries
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.aliases._

object FullOrder {
  type Response = Future[Root]
  type CcPayment = (OrderPayment, CreditCard, Region)

  final case class Totals(subTotal: Int, taxes: Int, shipping: Int, adjustments: Int, total: Int) extends ResponseItem

  object Totals {
    def empty: Totals = Totals(0,0,0,0,0)
  }

  final case class LineItems(
    skus: Seq[DisplayLineItem] = Seq.empty,
    giftCards: Seq[GiftCardResponse.Root] = Seq.empty
   ) extends ResponseItem

  final case class Root(
    id: Int,
    referenceNumber: String,
    orderState: Order.State,
    shippingState: Option[Order.State] = None,
    paymentState: CreditCardCharge.State,
    lineItems: LineItems,
    fraudScore: Int,
    totals: Totals,
    customer: Option[CustomerResponse.Root] = None,
    shippingMethod: Option[ShippingMethods.Root] = None,
    shippingAddress: Option[Addresses.Root] = None,
    remorsePeriodEnd: Option[Instant] = None,
    paymentMethods: Seq[Payments] = Seq.empty,
    lockedBy: Option[StoreAdmin]) extends ResponseItem with OrderResponseBase

  final case class DisplayLineItem(
    imagePath: String = "http://lorempixel.com/75/75/fashion",
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

    final case class CreditCardPayment(id: Int, customerId: Int, holderName: String, lastFour: String, expMonth: Int,
      expYear: Int, brand: String, address: Addresses.Root, `type`: Type = CreditCard) extends Payments

    final case class GiftCardPayment(code: String, amount: Int, currentBalance: Int, availableBalance: Int,
      createdAt: Instant, `type`: Type = GiftCard) extends Payments

    final case class StoreCreditPayment(id: Int, amount: Int, currentBalance: Int, availableBalance: Int,
      createdAt: Instant, `type`: Type = StoreCredit) extends Payments
  }

  def refreshAndFullOrder(order: Order)(implicit ec: EC, db: DB): DBIO[FullOrder.Root] =
    Orders.refresh(order).flatMap(fromOrder)

  def fromOrder(order: Order)(implicit ec: EC, db: DB): DBIO[Root] = {
    fetchOrderDetails(order).map {
      case (customer, lineItems, shipMethod, shipAddress, ccPmt, gcPmts, scPmts, gcs, totals, lockedBy, payState) ⇒
      build(
        order = order,
        customer = customer,
        lineItems = lineItems,
        giftCards = gcs,
        shippingAddress = shipAddress.toOption,
        shippingMethod = shipMethod,
        ccPmt = ccPmt,
        gcPmts = gcPmts,
        scPmts = scPmts,
        lockedBy = lockedBy,
        paymentState = payState,
        totals = totals
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
    paymentState: CreditCardCharge.State = CreditCardCharge.Cart): Root = {

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
      case data ⇒ { 
        val price = Mvp.priceAsInt(data.sku, data.skuShadow)
        val name = Mvp.name(data.sku, data.skuShadow).getOrElse("")
        DisplayLineItem(sku = data.sku.code, referenceNumber = data.lineItem.referenceNumber,
          state = data.lineItem.state, name = name, price = price, totalPrice = price)
      }
    }
    val gcList = giftCards.map { case (gc, li) ⇒ GiftCardResponse.build(gc) }

    Root(id = order.id,
      referenceNumber = order.referenceNumber,
      orderState = order.state,
      shippingState = order.getShippingState,
      paymentState = paymentState,
      lineItems = LineItems(skus = skuList, giftCards = gcList),
      fraudScore = scala.util.Random.nextInt(100),
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

  private def fetchOrderDetails(order: Order)(implicit ec: EC, db: DB) = {
    val ccPaymentQ = for {
      payment     ← OrderPayments.findAllByOrderId(order.id)
      creditCard  ← CreditCards.filter(_.id === payment.paymentMethodId)
      region      ← creditCard.region
    } yield (payment, creditCard, region)

    for {
      customer    ← Customers.findById(order.customerId).extract.one
      lineItemTup ← OrderLineItemSkus.findLineItemsByOrder(order).result
      lineItems =  lineItemTup.map { 
        case (sku, skuShadow, lineItem) ⇒ 
          OrderLineItemProductData(sku, skuShadow, lineItem)
      }
      giftCards   ← OrderLineItemGiftCards.findLineItemsByOrder(order).result
      shipMethod  ← shipping.ShippingMethods.forOrder(order).one
      shipAddress ← Addresses.forOrderId(order.id)
      payments    ← ccPaymentQ.one
      gcPayments  ← OrderPayments.findAllGiftCardsByOrderId(order.id).result
      scPayments  ← OrderPayments.findAllStoreCreditsByOrderId(order.id).result
      lockedBy    ← currentLock(order)
      payState    ← OrderQueries.getPaymentState(order.id)
    } yield (
      customer, 
      lineItems,
      shipMethod, 
      shipAddress, 
      payments, 
      gcPayments, 
      scPayments,
      giftCards, 
      Some(totals(order)), 
      lockedBy, 
      payState)
  }
}
