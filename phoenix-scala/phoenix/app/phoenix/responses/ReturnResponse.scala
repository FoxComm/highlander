package phoenix.responses

import java.time.Instant
import phoenix.models.account.{Organization, Organizations, Users}
import phoenix.models.admin.AdminsData
import phoenix.models.customer.CustomersData
import phoenix.models.payment.giftcard.GiftCard
import phoenix.models.returns.ReturnPayments.scope._
import phoenix.models.returns._
import phoenix.responses.CustomerResponse.{Root ⇒ Customer}
import phoenix.responses.StoreAdminResponse.{Root ⇒ User}
import phoenix.services.carts.CartTotaler
import phoenix.services.returns.{ReturnLineItemManager, ReturnTotaler}
import utils.Money._
import utils.db._

object ReturnResponse {
  case class ReturnTotals(subTotal: Int, taxes: Int, shipping: Int, adjustments: Int, total: Int)
      extends ResponseItem

  sealed trait LineItem extends ResponseItem {
    def id: Int
    def reason: String
    def price: Int
    def currency: Currency
  }
  object LineItem {
    case class Sku(id: Int,
                   reason: String,
                   imagePath: String,
                   title: String,
                   sku: String,
                   quantity: Int,
                   price: Int,
                   currency: Currency)
        extends LineItem
    case class ShippingCost(id: Int,
                            reason: String,
                            name: String,
                            amount: Int,
                            price: Int,
                            currency: Currency)
        extends LineItem
  }
  case class LineItems(skus: Seq[LineItem.Sku], shippingCosts: Option[LineItem.ShippingCost])
      extends ResponseItem

  sealed trait Payment extends ResponseItem {
    def id: Int
    def amount: Int
    def currency: Currency
  }
  object Payment {
    case class CreditCard(id: Int, amount: Int, currency: Currency)             extends Payment
    case class GiftCard(id: Int, code: String, amount: Int, currency: Currency) extends Payment
    case class StoreCredit(id: Int, amount: Int, currency: Currency)            extends Payment
  }
  case class Payments(creditCard: Option[Payment.CreditCard],
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
                  customer: Option[Customer],
                  storeAdmin: Option[User],
                  messageToCustomer: Option[String],
                  canceledReasonId: Option[Int],
                  createdAt: Instant,
                  updatedAt: Instant,
                  totals: ReturnTotals)
      extends ResponseItem

  def buildPayments(creditCard: Option[ReturnPayment],
                    giftCard: Option[(ReturnPayment, GiftCard)],
                    storeCredit: Option[ReturnPayment]): Payments =
    Payments(
        creditCard =
          creditCard.map(cc ⇒ Payment.CreditCard(cc.paymentMethodId, cc.amount, cc.currency)),
        giftCard = giftCard.map {
          case (p, gc) ⇒ Payment.GiftCard(p.paymentMethodId, gc.code, p.amount, p.currency)
        },
        storeCredit =
          storeCredit.map(sc ⇒ Payment.StoreCredit(sc.paymentMethodId, sc.amount, sc.currency))
    )

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
      storeAdmin   ← * <~ rma.storeAdminId.map(Users.findOneByAccountId).getOrElse(lift(None))
      adminData    ← * <~ rma.storeAdminId.map(AdminsData.findOneByAccountId).getOrElse(lift(None))
      organization ← * <~ rma.storeAdminId.map(Organizations.mustFindById404)
      // Payment methods
      ccPayment ← * <~ ReturnPayments.findAllByReturnId(rma.id).creditCards.one
      gcPayment ← * <~ ReturnPayments.findGiftCards(rma.id).one
      scPayment ← * <~ ReturnPayments.findAllByReturnId(rma.id).storeCredits.one
      // Line items of each subtype
      lineItems     ← * <~ ReturnLineItemManager.fetchSkuLineItems(rma)
      shippingCosts ← * <~ ReturnLineItemManager.fetchShippingCostLineItem(rma)
      // Totals
      adjustments ← * <~ ReturnTotaler.adjustmentsTotal(rma)
      subTotal    ← * <~ ReturnTotaler.subTotal(rma)
      shipping = shippingCosts.map(_.amount).getOrElse(0)
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
            a   ← storeAdmin
            ad  ← adminData
            org ← organization
          } yield StoreAdminResponse.build(a, ad, org),
          payments =
            buildPayments(creditCard = ccPayment, giftCard = gcPayment, storeCredit = scPayment),
          lineItems = LineItems(skus = lineItems, shippingCosts = shippingCosts),
          totals = buildTotals(subTotal = subTotal,
                               shipping = shipping,
                               adjustments = adjustments,
                               taxes = taxes)
      )
  }

  def build(rma: Return,
            customer: Option[Customer] = None,
            storeAdmin: Option[User] = None,
            lineItems: LineItems = LineItems(List.empty, Option.empty),
            payments: Payments = Payments(Option.empty, Option.empty, Option.empty),
            totals: ReturnTotals = ReturnTotals(0, 0, 0, 0, 0)): Root =
    Root(id = rma.id,
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
         totals = totals)
}
