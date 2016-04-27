package models.rma

import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCard, CreditCards}
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit
import models.stripe._
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.Money._
import utils.db._

case class RmaPayment(id: Int = 0, rmaId: Int = 0, amount: Int = 0,
  currency: Currency = Currency.USD, paymentMethodId: Int, paymentMethodType: PaymentMethod.Type)
  extends FoxModel[RmaPayment] {

  def isCreditCard:   Boolean = paymentMethodType == PaymentMethod.CreditCard
  def isGiftCard:     Boolean = paymentMethodType == PaymentMethod.GiftCard
  def isStoreCredit:  Boolean = paymentMethodType == PaymentMethod.StoreCredit
}

object RmaPayment {
  def fromStripeCustomer(stripeCustomer: StripeCustomer, rma: Rma): RmaPayment =
    RmaPayment(rmaId = rma.id, paymentMethodId = 1, paymentMethodType = PaymentMethod.CreditCard)

  def build(method: PaymentMethod, rmaId: Int, amount: Int, currency: Currency): RmaPayment = method match {
    case gc: GiftCard ⇒
      RmaPayment(rmaId = rmaId, amount = amount, currency = currency,
        paymentMethodId = gc.id, paymentMethodType = PaymentMethod.GiftCard)
    case cc: CreditCard ⇒
      RmaPayment(rmaId = rmaId, amount = amount, currency = currency,
        paymentMethodId = cc.id, paymentMethodType = PaymentMethod.CreditCard)
    case sc: StoreCredit ⇒
      RmaPayment(rmaId = rmaId, amount = amount, currency = currency,
        paymentMethodId = sc.id, paymentMethodType = PaymentMethod.StoreCredit)
  }

}

class RmaPayments(tag: Tag) extends FoxTable[RmaPayment](tag, "rma_payments") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def paymentMethodId = column[Int]("payment_method_id")
  def paymentMethodType = column[PaymentMethod.Type]("payment_method_type")
  def amount = column[Int]("amount")
  def currency = column[Currency]("currency")

  def * = (id, rmaId, amount, currency, paymentMethodId, paymentMethodType) <> ((RmaPayment.apply _).tupled,
    RmaPayment.unapply )

  def rma = foreignKey(Rmas.tableName, rmaId, Rmas)(_.id)
}

object RmaPayments extends FoxTableQuery[RmaPayment, RmaPayments](new RmaPayments(_))
  with ReturningId[RmaPayment, RmaPayments] {

  val returningLens: Lens[RmaPayment, Int] = lens[RmaPayment].id

  def findAllByRmaId(id: Int): QuerySeq =
    filter(_.rmaId === id)

  def findAllStoreCredit: QuerySeq =
    filter(_.paymentMethodType === (PaymentMethod.StoreCredit: PaymentMethod.Type))

  def findAllPaymentMethodmentsFor(rmaId: Int): Query[(RmaPayments, CreditCards), (RmaPayment, CreditCard), Seq] = {
    for {
      payments ← this.filter(_.rmaId === rmaId)
      cards    ← CreditCards if cards.id === payments.id
    } yield (payments, cards)
  }

  def findAllCreditCardsForOrder(rmaId: Rep[Int]): QuerySeq =
    filter(_.rmaId === rmaId).filter(_.paymentMethodType === (PaymentMethod.CreditCard: PaymentMethod.Type))

  object scope {
    implicit class RmaPaymentsQuerySeqConversions(q: QuerySeq) {
      def giftCards:    QuerySeq = q.byType(PaymentMethod.GiftCard)
      def creditCards:  QuerySeq = q.byType(PaymentMethod.CreditCard)
      def storeCredits: QuerySeq = q.byType(PaymentMethod.StoreCredit)

      def byType(pmt: PaymentMethod.Type): QuerySeq = q.filter(_.paymentMethodType === (pmt: PaymentMethod.Type))
    }
  }
}
