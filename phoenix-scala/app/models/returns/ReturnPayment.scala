package models.returns

import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCard, CreditCards}
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.Money._
import utils.aliases.stripe._
import utils.db._

case class ReturnPayment(id: Int = 0,
                         returnId: Int = 0,
                         amount: Int = 0,
                         currency: Currency = Currency.USD,
                         paymentMethodId: Int,
                         paymentMethodType: PaymentMethod.Type)
    extends FoxModel[ReturnPayment] {

  def isCreditCard: Boolean  = paymentMethodType == PaymentMethod.CreditCard
  def isGiftCard: Boolean    = paymentMethodType == PaymentMethod.GiftCard
  def isStoreCredit: Boolean = paymentMethodType == PaymentMethod.StoreCredit
}

object ReturnPayment {
  def fromStripeCustomer(stripeCustomer: StripeCustomer, rma: Return): ReturnPayment =
    ReturnPayment(returnId = rma.id,
                  paymentMethodId = 1,
                  paymentMethodType = PaymentMethod.CreditCard)

  def build(method: PaymentMethod, returnId: Int, amount: Int, currency: Currency): ReturnPayment =
    method match {
      case gc: GiftCard ⇒
        ReturnPayment(returnId = returnId,
                      amount = amount,
                      currency = currency,
                      paymentMethodId = gc.id,
                      paymentMethodType = PaymentMethod.GiftCard)
      case cc: CreditCard ⇒
        ReturnPayment(returnId = returnId,
                      amount = amount,
                      currency = currency,
                      paymentMethodId = cc.id,
                      paymentMethodType = PaymentMethod.CreditCard)
      case sc: StoreCredit ⇒
        ReturnPayment(returnId = returnId,
                      amount = amount,
                      currency = currency,
                      paymentMethodId = sc.id,
                      paymentMethodType = PaymentMethod.StoreCredit)
    }
}

class ReturnPayments(tag: Tag) extends FoxTable[ReturnPayment](tag, "return_payments") {
  def id                = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def returnId          = column[Int]("return_id")
  def paymentMethodId   = column[Int]("payment_method_id")
  def paymentMethodType = column[PaymentMethod.Type]("payment_method_type")
  def amount            = column[Int]("amount")
  def currency          = column[Currency]("currency")

  def * =
    (id, returnId, amount, currency, paymentMethodId, paymentMethodType) <> ((ReturnPayment.apply _).tupled,
        ReturnPayment.unapply)

  def rma = foreignKey(Returns.tableName, returnId, Returns)(_.id)
}

object ReturnPayments
    extends FoxTableQuery[ReturnPayment, ReturnPayments](new ReturnPayments(_))
    with ReturningId[ReturnPayment, ReturnPayments] {

  val returningLens: Lens[ReturnPayment, Int] = lens[ReturnPayment].id

  def findAllByReturnId(returnId: Int): QuerySeq =
    filter(_.returnId === returnId)

  object scope {
    implicit class RmaPaymentsQuerySeqConversions(q: QuerySeq) {
      def giftCards: QuerySeq    = q.byType(PaymentMethod.GiftCard)
      def creditCards: QuerySeq  = q.byType(PaymentMethod.CreditCard)
      def storeCredits: QuerySeq = q.byType(PaymentMethod.StoreCredit)

      def paymentMethodIds: Query[Rep[Int], Int, Set] = q.map(_.paymentMethodId).to[Set]

      def byType(pmt: PaymentMethod.Type): QuerySeq =
        q.filter(_.paymentMethodType === (pmt: PaymentMethod.Type))
    }
  }
}
