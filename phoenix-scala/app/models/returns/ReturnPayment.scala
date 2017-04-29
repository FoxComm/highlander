package models.returns

import models.cord.OrderPayments.QuerySeq
import models.payment.PaymentMethod
import models.payment.giftcard.{GiftCard, GiftCards}
import models.payment.storecredit.{StoreCredit, StoreCredits}
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.Money._
import utils.db._

case class ReturnPayment(id: Int = 0,
                         returnId: Int = 0,
                         amount: Int = 0,
                         currency: Currency = Currency.USD,
                         paymentMethodId: Int,
                         paymentMethodType: PaymentMethod.Type)
    extends FoxModel[ReturnPayment]

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
  import scope._

  val returningLens: Lens[ReturnPayment, Int] = lens[ReturnPayment].id

  def findAllByReturnId(returnId: Int): QuerySeq =
    filter(_.returnId === returnId)

  def findGiftCards(
      returnId: Int): Query[(ReturnPayments, GiftCards), (ReturnPayment, GiftCard), Seq] =
    findAllByReturnId(returnId).giftCards.join(GiftCards).on(_.paymentMethodId === _.id)

  def findStoreCredits(
      returnId: Int): Query[(ReturnPayments, StoreCredits), (ReturnPayment, StoreCredit), Seq] =
    findAllByReturnId(returnId).storeCredits.join(StoreCredits).on(_.paymentMethodId === _.id)

  def findOnHoldGiftCards(returnId: Int): GiftCards.QuerySeq =
    findGiftCards(returnId).map { case (_, gc) ⇒ gc }
      .filter(_.state === (GiftCard.OnHold: GiftCard.State))

  def findOnHoldStoreCredits(returnId: Int): StoreCredits.QuerySeq =
    findStoreCredits(returnId).map { case (_, sc) ⇒ sc }
      .filter(_.state === (StoreCredit.OnHold: StoreCredit.State))

  object scope {
    implicit class RmaPaymentsQuerySeqConversions(private val q: QuerySeq) extends AnyVal {
      def giftCards: QuerySeq    = q.byType(PaymentMethod.GiftCard)
      def storeCredits: QuerySeq = q.byType(PaymentMethod.StoreCredit)

      // todo external here is enough?
      def creditCards: QuerySeq = q.byType(PaymentMethod.CreditCard)
      def externalPayments: QuerySeq =
        q.filter(_.paymentMethodType.inSet(Set(PaymentMethod.CreditCard, PaymentMethod.ApplePay)))

      def paymentMethodIds: Query[Rep[Int], Int, Set] = q.map(_.paymentMethodId).to[Set]

      def byType(pmt: PaymentMethod.Type): QuerySeq =
        q.filter(_.paymentMethodType === (pmt: PaymentMethod.Type))
    }
  }
}
