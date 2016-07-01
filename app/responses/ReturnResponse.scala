package responses

import java.time.Instant

import models.customer.Customers
import models.inventory.Sku
import models.objects._
import models.product.Mvp
import models.order.Orders
import models.payment.PaymentMethod
import models.payment.giftcard.GiftCard
import models.returns._
import models.shipping.Shipment
import models.StoreAdmins
import responses.CustomerResponse.{Root ⇒ Customer}
import responses.StoreAdminResponse.{Root ⇒ StoreAdmin}
import responses.order.FullOrder
import services.returns.ReturnTotaler
import utils.Money._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object ReturnResponse {
  case class ReturnTotals(subTotal: Int, shipping: Int, taxes: Int, total: Int)
      extends ResponseItem

  case class LineItemSku(lineItemId: Int, sku: DisplaySku) extends ResponseItem
  case class LineItemGiftCard(lineItemId: Int, giftCard: GiftCardResponse.Root)
      extends ResponseItem
  case class LineItemShippingCost(lineItemId: Int, shippingCost: ShipmentResponse.Root)
      extends ResponseItem

  case class LineItems(skus: Seq[LineItemSku] = Seq.empty,
                       giftCards: Seq[LineItemGiftCard] = Seq.empty,
                       shippingCosts: Seq[LineItemShippingCost] = Seq.empty)
      extends ResponseItem

  case class DisplayPayment(id: Int,
                            amount: Int,
                            currency: Currency = Currency.USD,
                            paymentMethodId: Int,
                            paymentMethodType: PaymentMethod.Type)
      extends ResponseItem

  case class DisplaySku(imagePath: String = "http://lorempixel.com/75/75/fashion",
                        name: String = "donkey product",
                        sku: String,
                        price: Int = 33,
                        quantity: Int = 1,
                        totalPrice: Int = 33)
      extends ResponseItem

  case class Root(id: Int,
                  referenceNumber: String,
                  orderRefNum: String,
                  rmaType: Return.ReturnType,
                  state: Return.State,
                  lineItems: LineItems,
                  payments: Seq[DisplayPayment],
                  customer: Option[Customer] = None,
                  storeAdmin: Option[StoreAdmin] = None,
                  messageToCustomer: Option[String] = None,
                  canceledReason: Option[Int] = None,
                  createdAt: Instant,
                  updatedAt: Instant,
                  totals: Option[ReturnTotals])
      extends ResponseItem

  case class RootExpanded(id: Int,
                          referenceNumber: String,
                          order: Option[FullOrder.Root],
                          rmaType: Return.ReturnType,
                          state: Return.State,
                          lineItems: LineItems,
                          payments: Seq[DisplayPayment],
                          customer: Option[Customer] = None,
                          storeAdmin: Option[StoreAdmin] = None,
                          messageToCustomer: Option[String] = None,
                          canceledReason: Option[Int] = None,
                          createdAt: Instant,
                          updatedAt: Instant,
                          totals: Option[ReturnTotals])
      extends ResponseItem

  def buildPayment(pmt: ReturnPayment): DisplayPayment =
    DisplayPayment(
        id = pmt.id,
        amount = pmt.amount,
        currency = pmt.currency,
        paymentMethodId = pmt.paymentMethodId,
        paymentMethodType = pmt.paymentMethodType
    )

  def buildLineItems(skus: Seq[(Sku, ObjectForm, ObjectShadow, ReturnLineItem)],
                     giftCards: Seq[(GiftCard, ReturnLineItem)],
                     shipments: Seq[(Shipment, ReturnLineItem)]): LineItems = {
    LineItems(
        skus = skus.map {
          case (sku, form, shadow, li) ⇒
            LineItemSku(lineItemId = li.id,
                        sku = DisplaySku(sku = sku.code, price = Mvp.priceAsInt(form, shadow)))
        },
        giftCards = giftCards.map {
          case (gc, li) ⇒
            LineItemGiftCard(lineItemId = li.id, giftCard = GiftCardResponse.build(gc))
        },
        shippingCosts = shipments.map {
          case (shipment, li) ⇒
            LineItemShippingCost(lineItemId = li.id,
                                 shippingCost = ShipmentResponse.build(shipment))
        }
    )
  }

  def buildTotals(subtotal: Option[Int],
                  taxes: Option[Int],
                  shipments: Seq[(Shipment, ReturnLineItem)]): ReturnTotals = {
    val finalSubtotal = subtotal.getOrElse(0)
    val finalTaxes    = taxes.getOrElse(0)
    val finalShipping = shipments.foldLeft(0) {
      case (acc, (shipment, li)) ⇒ acc + shipment.shippingPrice.getOrElse(0)
    }
    val grandTotal = finalSubtotal + finalShipping + finalTaxes
    ReturnTotals(finalSubtotal, finalTaxes, finalShipping, grandTotal)
  }

  def fromRma(rma: Return)(implicit ec: EC, db: DB): DbResultT[Root] = {
    fetchRmaDetails(rma).map {
      case (_, customer, storeAdmin, payments, lineItemData, giftCards, shipments, subtotal) ⇒
        build(
            rma = rma,
            customer = customer.map(CustomerResponse.build(_)),
            storeAdmin = storeAdmin.map(StoreAdminResponse.build),
            payments = payments.map(buildPayment),
            lineItems = buildLineItems(lineItemData, giftCards, shipments),
            totals = Some(buildTotals(subtotal, None, shipments))
        )
    }
  }

  def fromRmaExpanded(rma: Return)(implicit ec: EC, db: DB): DbResultT[RootExpanded] = {
    fetchRmaDetails(rma = rma, withOrder = true).map {
      case (order, customer, storeAdmin, payments, lineItemData, giftCards, shipments, subtotal) ⇒
        buildExpanded(
            rma = rma,
            order = order,
            customer = customer.map(CustomerResponse.build(_)),
            storeAdmin = storeAdmin.map(StoreAdminResponse.build),
            payments = payments.map(buildPayment),
            lineItems = buildLineItems(lineItemData, giftCards, shipments),
            totals = Some(buildTotals(subtotal, None, shipments))
        )
    }
  }

  def build(rma: Return,
            customer: Option[Customer] = None,
            storeAdmin: Option[StoreAdmin] = None,
            lineItems: LineItems = LineItems(),
            payments: Seq[DisplayPayment] = Seq.empty,
            totals: Option[ReturnTotals] = None): Root =
    Root(id = rma.id,
         referenceNumber = rma.refNum,
         orderRefNum = rma.orderRef,
         rmaType = rma.returnType,
         state = rma.state,
         customer = customer,
         storeAdmin = storeAdmin,
         payments = payments,
         lineItems = lineItems,
         messageToCustomer = rma.messageToCustomer,
         canceledReason = rma.canceledReason,
         createdAt = rma.createdAt,
         updatedAt = rma.updatedAt,
         totals = totals)

  def buildExpanded(rma: Return,
                    order: Option[FullOrder.Root] = None,
                    customer: Option[Customer] = None,
                    lineItems: LineItems = LineItems(),
                    storeAdmin: Option[StoreAdmin] = None,
                    payments: Seq[DisplayPayment] = Seq.empty,
                    totals: Option[ReturnTotals] = None): RootExpanded =
    RootExpanded(
        id = rma.id,
        referenceNumber = rma.refNum,
        order = order,
        rmaType = rma.returnType,
        state = rma.state,
        customer = customer,
        storeAdmin = storeAdmin,
        payments = payments,
        lineItems = lineItems,
        messageToCustomer = rma.messageToCustomer,
        canceledReason = rma.canceledReason,
        createdAt = rma.createdAt,
        updatedAt = rma.updatedAt,
        totals = totals
    )

  private def fetchRmaDetails(rma: Return, withOrder: Boolean = false)(implicit db: DB, ec: EC) = {
    val orderQ: DbResultT[Option[FullOrder.Root]] = for {
      maybeOrder ← * <~ Orders.findByRefNum(rma.orderRef).one
      fullOrder ← * <~ ((maybeOrder, withOrder) match {
                       case (Some(order), true) ⇒ FullOrder.fromOrder(order).map(Some(_))
                       case _                   ⇒ DbResultT.none[FullOrder.Root]
                     })
    } yield fullOrder

    for {
      // Order, if necessary
      fullOrder ← * <~ orderQ
      // Either customer or storeAdmin as creator
      customer ← * <~ Customers.findById(rma.customerId).extract.one
      storeAdmin ← * <~ rma.storeAdminId
                    .map(id ⇒ StoreAdmins.findById(id).extract.one)
                    .getOrElse(lift(None))
      // Payment methods
      payments ← * <~ ReturnPayments.filter(_.returnId === rma.id).result
      // Line items of each subtype
      lineItems ← * <~ ReturnLineItemSkus.findLineItemsByRma(rma).result
      giftCards ← * <~ ReturnLineItemGiftCards.findLineItemsByRma(rma).result
      shipments ← * <~ ReturnLineItemShippingCosts.findLineItemsByRma(rma).result
      // Subtotal
      subtotal ← * <~ ReturnTotaler.subTotal(rma)
    } yield (fullOrder, customer, storeAdmin, payments, lineItems, giftCards, shipments, subtotal)
  }
}
