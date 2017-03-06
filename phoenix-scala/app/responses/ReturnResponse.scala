package responses

import java.time.Instant
import models.account.Users
import models.customer.CustomersData
import models.admin.AdminsData
import models.inventory.Sku
import models.objects._
import models.payment.PaymentMethod
import models.payment.giftcard.GiftCard
import models.product.Mvp
import models.returns._
import responses.CustomerResponse.{Root ⇒ Customer}
import responses.StoreAdminResponse.{Root ⇒ User}
import services.carts.CartTotaler
import services.returns.ReturnTotaler
import slick.driver.PostgresDriver.api._
import utils.Money._
import utils.aliases._
import utils.db._

object ReturnResponse {
  case class ReturnTotals(subTotal: Int, taxes: Int, shipping: Int, adjustments: Int, total: Int)
      extends ResponseItem

  case class LineItemSku(lineItemId: Int, sku: DisplaySku) extends ResponseItem
  case class LineItemGiftCard(lineItemId: Int, giftCard: GiftCardResponse.Root)
      extends ResponseItem
  case class LineItemShippingCost(lineItemId: Int, amount: Int) extends ResponseItem

  case class LineItems(skus: Seq[LineItemSku] = Seq.empty,
                       giftCards: Seq[LineItemGiftCard] = Seq.empty,
                       shippingCosts: Option[LineItemShippingCost] = Option.empty)
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
                  cordRefNum: String,
                  rmaType: Return.ReturnType,
                  state: Return.State,
                  lineItems: LineItems,
                  payments: Seq[DisplayPayment],
                  customer: Option[Customer] = None,
                  storeAdmin: Option[User] = None,
                  messageToCustomer: Option[String] = None,
                  canceledReasonId: Option[Int] = None,
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

  def buildLineItems(
      skus: Seq[(Sku, ObjectForm, ObjectShadow, ReturnLineItem)],
      giftCards: Seq[(GiftCard, ReturnLineItem)],
      shippingCosts: Option[(ReturnLineItemShippingCost, ReturnLineItem)]): LineItems = {
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
        shippingCosts = shippingCosts.map {
          case (costs, li) ⇒
            LineItemShippingCost(lineItemId = li.id, amount = costs.amount)
        }
    )
  }

  def buildTotals(subTotal: Int, shipping: Int, adjustments: Int, taxes: Int): ReturnTotals = {
    ReturnTotals(subTotal = subTotal, shipping = shipping, adjustments = adjustments, taxes = taxes, total = subTotal + shipping + taxes - adjustments)
  }

  def fromRma(rma: Return)(implicit ec: EC, db: DB): DbResultT[Root] = {
    for {
      // Either customer or storeAdmin as creator
      customer     ← * <~ Users.findOneByAccountId(rma.accountId)
      customerData ← * <~ CustomersData.findOneByAccountId(rma.accountId)
      storeAdmin ← * <~ rma.storeAdminId
                    .map(Users.findOneByAccountId)
                    .getOrElse(lift(None))
      adminData ← * <~ rma.storeAdminId
                   .map(AdminsData.findOneByAccountId)
                   .getOrElse(lift(None))
      // Payment methods
      payments ← * <~ ReturnPayments.filter(_.returnId === rma.id).result
      // Line items of each subtype
      lineItems     ← * <~ ReturnLineItemSkus.findLineItemsByRma(rma)
      giftCards     ← * <~ ReturnLineItemGiftCards.findLineItemsByRma(rma)
      shippingCosts ← * <~ ReturnLineItemShippingCosts.findLineItemByRma(rma)
      // Totals
      adjustments <- * <~ ReturnTotaler.adjustmentsTotal(rma)
      subTotal ← * <~ ReturnTotaler.subTotal(rma)
      shipping = shippingCosts.map { case (rli, _) ⇒ rli.amount }.getOrElse(0)
      taxes ← * <~ CartTotaler.taxesTotal(rma.orderRef,
                                          subTotal = subTotal,
                                          shipping = shipping,
                                          adjustments = adjustments)
    } yield
      build(
          rma = rma,
          customer = for {
            c  ← customer
            cu ← customerData
          } yield CustomerResponse.build(c, cu),
          storeAdmin = for {
            a  ← storeAdmin
            au ← adminData
          } yield StoreAdminResponse.build(a, au),
          payments = payments.map(buildPayment),
          lineItems = buildLineItems(lineItems, giftCards, shippingCosts),
          totals = Some(buildTotals(subTotal = subTotal, shipping = shipping, adjustments = adjustments, taxes = taxes))
      )
  }

  def build(rma: Return,
            customer: Option[Customer] = None,
            storeAdmin: Option[User] = None,
            lineItems: LineItems = LineItems(),
            payments: Seq[DisplayPayment] = Seq.empty,
            totals: Option[ReturnTotals] = None): Root =
    Root(id = rma.id,
         referenceNumber = rma.refNum,
         cordRefNum = rma.orderRef,
         rmaType = rma.returnType,
         state = rma.state,
         customer = customer,
         storeAdmin = storeAdmin,
         payments = payments,
         lineItems = lineItems,
         messageToCustomer = rma.messageToAccount,
         canceledReasonId = rma.canceledReasonId,
         createdAt = rma.createdAt,
         updatedAt = rma.updatedAt,
         totals = totals)
}
