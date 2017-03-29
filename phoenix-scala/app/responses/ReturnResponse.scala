package responses

import java.time.Instant
import models.account.Users
import models.customer.CustomersData
import models.admin.AdminsData
import models.inventory.Sku
import models.objects._
import models.payment.giftcard.{GiftCard, GiftCards}
import models.product.Mvp
import models.returns._
import models.returns.ReturnPayments.scope._
import responses.CustomerResponse.{Root => Customer}
import responses.StoreAdminResponse.{Root => User}
import services.carts.CartTotaler
import services.returns.ReturnTotaler
import slick.driver.PostgresDriver.api._
import utils.Money._
import utils.aliases._
import utils.db._

object ReturnResponse {
  case class ReturnTotals(subTotal: Int, taxes: Int, shipping: Int, adjustments: Int, total: Int)
      extends ResponseItem

  sealed trait LineItem extends ResponseItem {
    def id: Int
  }
  case class LineItemSku(id: Int, sku: DisplaySku) extends LineItem
  case class LineItemShippingCost(id: Int, amount: Int) extends LineItem

  case class LineItems(skus: Seq[LineItemSku] = Seq.empty,
                       shippingCosts: Option[LineItemShippingCost] = Option.empty)
      extends ResponseItem

  sealed trait Payment extends ResponseItem {
    def id: Int
    def amount: Int
    def currency: Currency
  }
  object Payment {
    case class CreditCard(id: Int, amount: Int, currency: Currency) extends Payment
    case class GiftCard(id: Int, code: String, amount: Int, currency: Currency) extends Payment
    case class StoreCredit(id: Int, amount: Int, currency: Currency) extends Payment
  }
  case class Payments(creditCard: Option[Payment.CreditCard] = None,
                      giftCard: Option[Payment.GiftCard] = None,
                      storeCredit: Option[Payment.StoreCredit] = None) extends ResponseItem

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
                  payments: Payments,
                  customer: Option[Customer] = None,
                  storeAdmin: Option[User] = None,
                  messageToCustomer: Option[String] = None,
                  canceledReasonId: Option[Int] = None,
                  createdAt: Instant,
                  updatedAt: Instant,
                  totals: Option[ReturnTotals])
      extends ResponseItem

  def buildPayments(creditCard: Option[ReturnPayment], giftCard: Option[(ReturnPayment, GiftCard)], storeCredit: Option[ReturnPayment]): Payments =
    Payments(
      creditCard = creditCard.map(cc ⇒ Payment.CreditCard(cc.paymentMethodId, cc.amount, cc.currency)),
      giftCard = giftCard.map { case (p, gc) ⇒ Payment.GiftCard(p.paymentMethodId, gc.code, p.amount, p.currency) },
      storeCredit = storeCredit.map(sc ⇒ Payment.StoreCredit(sc.paymentMethodId, sc.amount, sc.currency))
    )

  def buildLineItems(
      skus: Seq[(Sku, ObjectForm, ObjectShadow, ReturnLineItem)],
      shippingCosts: Option[(ReturnLineItemShippingCost, ReturnLineItem)]): LineItems = {
    LineItems(
        skus = skus.map {
          case (sku, form, shadow, li) ⇒
            LineItemSku(id = li.id,
                        sku = DisplaySku(sku = sku.code, price = Mvp.priceAsInt(form, shadow)))
        },
        shippingCosts = shippingCosts.map {
          case (costs, li) ⇒
            LineItemShippingCost(id = li.id, amount = costs.amount)
        }
    )
  }

  def buildTotals(subTotal: Int, shipping: Int, adjustments: Int, taxes: Int): ReturnTotals = {
    ReturnTotals(subTotal = subTotal,
                 shipping = shipping,
                 adjustments = adjustments,
                 taxes = taxes,
                 total = subTotal + shipping + taxes - adjustments)
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
      ccPayment <- * <~ ReturnPayments.findAllByReturnId(rma.id).creditCards.one
      gcPayment <- * <~ ReturnPayments.findGiftCards(rma.id).one
      scPayment <- * <~ ReturnPayments.findAllByReturnId(rma.id).storeCredits.one
      // Line items of each subtype
      lineItems     ← * <~ ReturnLineItemSkus.findLineItemsByRma(rma)
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
          payments = buildPayments(creditCard = ccPayment, giftCard = gcPayment, storeCredit = scPayment),
          lineItems = buildLineItems(lineItems, shippingCosts),
          totals = Some(buildTotals(subTotal = subTotal, shipping = shipping, adjustments = adjustments, taxes = taxes))
      )
  }

  def build(rma: Return,
            customer: Option[Customer] = None,
            storeAdmin: Option[User] = None,
            lineItems: LineItems = LineItems(),
            payments: Payments = Payments(),
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
