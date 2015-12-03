package responses

import java.time.Instant

import cats.implicits._
import models.{Adjustment, CreditCard, CreditCardCharge, CreditCards, Customer, Customers, GiftCard, Order,
OrderAssignment, OrderAssignments, OrderLineItem, OrderLineItemGiftCard, OrderLineItemGiftCards, OrderLineItemSkus,
OrderPayment, OrderPayments, Orders, PaymentMethod, Region, ShippingMethod, Sku, StoreAdmin, StoreAdmins, StoreCredit}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

import scala.concurrent.{ExecutionContext, Future}

object FullOrder {
  type Response = Future[Root]
  type CcPayment = (OrderPayment, CreditCard, Region)

  final case class Totals(subTotal: Int, taxes: Int, adjustments: Int, total: Int) extends ResponseItem

  final case class LineItems(
    skus: Seq[DisplayLineItem] = Seq.empty,
    giftCards: Seq[GiftCardResponse.Root] = Seq.empty
   ) extends ResponseItem

  final case class Root(
    id: Int,
    referenceNumber: String,
    orderStatus: Order.Status,
    shippingStatus: Order.Status,
    paymentStatus: Option[CreditCardCharge.Status] = None,
    lineItems: LineItems,
    adjustments: Seq[Adjustment],
    fraudScore: Int,
    totals: Totals,
    customer: Option[Customer] = None,
    shippingMethod: Option[ShippingMethods.Root] = None,
    shippingAddress: Option[Addresses.Root] = None,
    assignees: Seq[AssignmentResponse.Root] = Seq.empty,
    remorsePeriodEnd: Option[Instant] = None,
    paymentMethods: Seq[Payments] = Seq.empty) extends ResponseItem

  final case class DisplayLineItem(
    imagePath: String = "http://lorempixel.com/75/75/fashion",
    name: String = "donkey product",
    sku: String,
    price: Int = 33,
    quantity: Int = 1,
    totalPrice: Int = 33,
    status: OrderLineItem.Status) extends ResponseItem

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

  def refreshAndFullOrder(order: Order)(implicit ec: ExecutionContext, db: Database): DBIO[FullOrder.Root] =
    Orders.refresh(order).flatMap(fromOrder)

  def fromOrder(order: Order)(implicit ec: ExecutionContext, db: Database): DBIO[Root] = {
    fetchOrderDetails(order).map {
      case (customer, skus, shipMethod, shipAddress, ccPmt, gcPmts, scPmts, assignees, giftCards) ⇒
      build(
        order = order,
        customer = customer,
        skus = skus,
        giftCards = giftCards,
        shippingAddress = shipAddress.toOption,
        shippingMethod = shipMethod,
        assignments = assignees,
        ccPmt = ccPmt,
        gcPmts = gcPmts,
        scPmts = scPmts
      )
    }
  }

  def build(order: Order, skus: Seq[(Sku, OrderLineItem)] = Seq.empty, adjustments: Seq[Adjustment] = Seq.empty,
    shippingMethod: Option[ShippingMethod] = None, customer: Option[Customer] = None,
    shippingAddress: Option[Addresses.Root] = None,
    ccPmt: Option[CcPayment] = None, gcPmts: Seq[(OrderPayment, GiftCard)] = Seq.empty,
    scPmts: Seq[(OrderPayment, StoreCredit)] = Seq.empty,
    assignments: Seq[(OrderAssignment, StoreAdmin)] = Seq.empty,
    giftCards: Seq[(GiftCard, OrderLineItemGiftCard)] = Seq.empty): Root = {

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

    val skuList = skus.map { case (sku, li) ⇒
      DisplayLineItem(sku = sku.sku, status = li.status, name = sku.name.getOrElse("donkey product"),
        price = sku.price, totalPrice = sku.price)
    }
    val gcList = giftCards.map { case (gc, li) ⇒ GiftCardResponse.build(gc) }

    Root(id = order.id,
      referenceNumber = order.referenceNumber,
      orderStatus = order.status,
      shippingStatus = order.status,
      paymentStatus = none,
      //paymentStatus = maybePayment.map(p ⇒ getPaymentStatus(order.status, p)),
      lineItems = LineItems(skus = skuList, giftCards = gcList),
      adjustments = adjustments,
      fraudScore = scala.util.Random.nextInt(100),
      customer = customer,
      shippingAddress = shippingAddress,
      totals = Totals(subTotal = 333, taxes = 10, adjustments = 0, total = 510),
      shippingMethod = shippingMethod.map(ShippingMethods.build(_)),
      assignees = assignments.map((AssignmentResponse.build _).tupled),
      remorsePeriodEnd = order.getRemorsePeriodEnd,
      paymentMethods = paymentMethods
    )
  }

  private def fetchOrderDetails(order: Order)(implicit ec: ExecutionContext) = {
    val shippingMethodQ = for {
      orderShippingMethod ← models.OrderShippingMethods.filter(_.orderId === order.id)
      shipMethod ← models.ShippingMethods.filter(_.id === orderShippingMethod.shippingMethodId)
    } yield shipMethod

    val ccPaymentQ = for {
      payment     ← OrderPayments.findAllByOrderId(order.id)
      creditCard  ← CreditCards.filter(_.id === payment.paymentMethodId)
      region      ← creditCard.region
      //creditCardCharge ← CreditCardCharges.filter(_.orderPaymentId === payment.id)
    } yield (payment, creditCard, region)//, creditCardCharge)

    for {
      customer ← Customers.findById(order.customerId).extract.one
      lineItems ← OrderLineItemSkus.findLineItemsByOrder(order).result
      giftCards ← OrderLineItemGiftCards.findLineItemsByOrder(order).result
      shipMethod ← shippingMethodQ.one
      shipAddress ← Addresses.forOrderId(order.id)
      payments ← ccPaymentQ.one
      gcPayments ← OrderPayments.findAllGiftCardsByOrderId(order.id).result
      scPayments ← OrderPayments.findAllStoreCreditsByOrderId(order.id).result
      assignments ← OrderAssignments.filter(_.orderId === order.id).result
      admins ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.assigneeId))).result
    } yield (customer, lineItems, shipMethod, shipAddress, payments, gcPayments, scPayments, assignments.zip(admins),
      giftCards)
  }
}
