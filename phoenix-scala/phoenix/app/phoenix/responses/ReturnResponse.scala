package phoenix.responses

import java.time.Instant

import core.db._
import core.utils.Money._
import phoenix.models.account.Users
import phoenix.models.admin.AdminsData
import phoenix.models.customer.CustomersData
import phoenix.models.payment.giftcard.GiftCard
import phoenix.models.returns.ReturnPayments.scope._
import phoenix.models.returns._
import phoenix.responses.users.{CustomerResponse, StoreAdminResponse}
import phoenix.services.carts.CartTotaler
import phoenix.services.returns.{ReturnLineItemManager, ReturnTotaler}

object ReturnResponse {
  case class ReturnTotals(subTotal: Long, taxes: Long, shipping: Long, adjustments: Long, total: Long)
      extends ResponseItem

  sealed trait LineItem extends ResponseItem {
    def id: Int
    def reason: String
    def price: Long
    def currency: Currency
  }
  object LineItem {
    case class Sku(id: Int,
                   reason: String,
                   imagePath: String,
                   title: String,
                   sku: String,
                   quantity: Int,
                   price: Long,
                   currency: Currency)
        extends LineItem
    case class ShippingCost(id: Int,
                            reason: String,
                            name: String,
                            amount: Long,
                            price: Long,
                            currency: Currency)
        extends LineItem
  }
  case class LineItems(skus: Seq[LineItem.Sku], shippingCosts: Option[LineItem.ShippingCost])
      extends ResponseItem

  sealed trait Payment extends ResponseItem {
    def id: Int
    def amount: Long
    def currency: Currency
  }
  object Payment {
    case class CreditCard(id: Int, amount: Long, currency: Currency)             extends Payment
    case class GiftCard(id: Int, code: String, amount: Long, currency: Currency) extends Payment
    case class StoreCredit(id: Int, amount: Long, currency: Currency)            extends Payment
    case class ApplePay(id: Int, amount: Long, currency: Currency)               extends Payment
  }
  case class Payments(creditCard: Option[Payment.CreditCard],
                      applePay: Option[Payment.ApplePay],
                      giftCard: Option[Payment.GiftCard],
                      storeCredit: Option[Payment.StoreCredit])
      extends ResponseItem

  case class Root(id: Int,
                  referenceNumber: String,
                  orderRefNum: String,
                  rmaType: Return.ReturnType,
                  state: Return.State,
                  lineItems: LineItems,
                  payments: Payments,
                  customer: Option[CustomerResponse],
                  storeAdmin: Option[StoreAdminResponse],
                  messageToCustomer: Option[String],
                  canceledReasonId: Option[Int],
                  createdAt: Instant,
                  updatedAt: Instant,
                  totals: ReturnTotals)
      extends ResponseItem

  def buildPayments(creditCard: Option[ReturnPayment],
                    applePay: Option[ReturnPayment],
                    giftCard: Option[(ReturnPayment, GiftCard)],
                    storeCredit: Option[ReturnPayment]): Payments =
    Payments(
      creditCard = creditCard.map(cc ⇒ Payment.CreditCard(cc.paymentMethodId, cc.amount, cc.currency)),
      applePay = applePay.map(ap ⇒ Payment.ApplePay(ap.paymentMethodId, ap.amount, ap.currency)),
      giftCard = giftCard.map {
        case (p, gc) ⇒ Payment.GiftCard(p.paymentMethodId, gc.code, p.amount, p.currency)
      },
      storeCredit = storeCredit.map(sc ⇒ Payment.StoreCredit(sc.paymentMethodId, sc.amount, sc.currency))
    )

  def buildTotals(subTotal: Long, shipping: Long, adjustments: Long, taxes: Long): ReturnTotals =
    ReturnTotals(subTotal = subTotal,
                 shipping = shipping,
                 adjustments = adjustments,
                 taxes = taxes,
                 total = subTotal + shipping + taxes - adjustments)

  def fromRma(rma: Return)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      // Either customer or storeAdmin as creator
      customer     ← * <~ Users.findOneByAccountId(rma.accountId)
      customerData ← * <~ CustomersData.findOneByAccountId(rma.accountId)
      storeAdmin   ← * <~ rma.storeAdminId.map(Users.findOneByAccountId).getOrElse(lift(None))
      adminData    ← * <~ rma.storeAdminId.map(AdminsData.findOneByAccountId).getOrElse(lift(None))
      // Payment methods
      ccPayment       ← * <~ ReturnPayments.findAllByReturnId(rma.id).creditCards.one
      applePayPayment ← * <~ ReturnPayments.findAllByReturnId(rma.id).applePays.one
      gcPayment       ← * <~ ReturnPayments.findGiftCards(rma.id).one
      scPayment       ← * <~ ReturnPayments.findAllByReturnId(rma.id).storeCredits.one
      // Line items of each subtype
      lineItems     ← * <~ ReturnLineItemManager.fetchSkuLineItems(rma)
      shippingCosts ← * <~ ReturnLineItemManager.fetchShippingCostLineItem(rma)
      // Totals
      adjustments ← * <~ ReturnTotaler.adjustmentsTotal(rma)
      subTotal    ← * <~ ReturnTotaler.subTotal(rma)
      shipping = shippingCosts.map(_.amount).getOrElse(0L)
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
        payments = buildPayments(creditCard = ccPayment,
                                 applePay = applePayPayment,
                                 giftCard = gcPayment,
                                 storeCredit = scPayment),
        lineItems = LineItems(skus = lineItems, shippingCosts = shippingCosts),
        totals =
          buildTotals(subTotal = subTotal, shipping = shipping, adjustments = adjustments, taxes = taxes)
      )

  def build(rma: Return,
            customer: Option[CustomerResponse] = None,
            storeAdmin: Option[StoreAdminResponse] = None,
            lineItems: LineItems = LineItems(List.empty, Option.empty),
            payments: Payments = Payments(Option.empty, Option.empty, Option.empty, Option.empty),
            totals: ReturnTotals = ReturnTotals(0, 0, 0, 0, 0)): Root =
    Root(
      id = rma.id,
      referenceNumber = rma.refNum,
      orderRefNum = rma.orderRef,
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
      totals = totals
    )
}
