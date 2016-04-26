package models.order

import cats.data.ValidatedNel
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCard, CreditCards}
import models.payment.giftcard.{GiftCard, GiftCards}
import models.payment.storecredit.{StoreCredit, StoreCredits}
import models.stripe._
import shapeless._
import failures.Failure
import utils.Money._
import utils.Validation._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class OrderPayment(id: Int = 0, orderId: Int = 0, amount: Option[Int] = None,
  currency: Currency = Currency.USD, paymentMethodId: Int, paymentMethodType: PaymentMethod.Type)
  extends FoxModel[OrderPayment] {

  def isCreditCard:   Boolean = paymentMethodType == PaymentMethod.CreditCard
  def isGiftCard:     Boolean = paymentMethodType == PaymentMethod.GiftCard
  def isStoreCredit:  Boolean = paymentMethodType == PaymentMethod.StoreCredit

  override def validate: ValidatedNel[Failure, OrderPayment] = {
    val amountOk = paymentMethodType match {
      case PaymentMethod.StoreCredit | PaymentMethod.GiftCard ⇒
        validExpr(amount.getOrElse(0) > 0, s"amount must be > 0 for ${paymentMethodType}")
      case PaymentMethod.CreditCard ⇒
        validExpr(amount.isEmpty, "amount must be empty for creditCard")
    }

    amountOk.map(_ ⇒ this)
  }
}

object OrderPayment {
  def fromStripeCustomer(stripeCustomer: StripeCustomer, order: Order): OrderPayment =
    OrderPayment(orderId = order.id, paymentMethodId = 1, paymentMethodType = PaymentMethod.CreditCard)

  def build(method: PaymentMethod): OrderPayment = method match {
    case gc: GiftCard ⇒
      OrderPayment(paymentMethodId = gc.id, paymentMethodType = PaymentMethod.GiftCard)
    case cc: CreditCard ⇒
      OrderPayment(paymentMethodId = cc.id, paymentMethodType = PaymentMethod.CreditCard)
    case sc: StoreCredit ⇒
      OrderPayment(paymentMethodId = sc.id, paymentMethodType = PaymentMethod.StoreCredit)
  }

}

class OrderPayments(tag: Tag) extends FoxTable[OrderPayment](tag, "order_payments") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def paymentMethodId = column[Int]("payment_method_id")
  def paymentMethodType = column[PaymentMethod.Type]("payment_method_type")
  def amount = column[Option[Int]]("amount")
  def currency = column[Currency]("currency")

  def * = (id, orderId, amount, currency, paymentMethodId, paymentMethodType) <> ((OrderPayment.apply _).tupled,
    OrderPayment.unapply )

  def order       = foreignKey(Orders.tableName, orderId, Orders)(_.id)
  def creditCard  = foreignKey(CreditCards.tableName, paymentMethodId, CreditCards)(_.id)
}

object OrderPayments extends FoxTableQuery[OrderPayment, OrderPayments](
  idLens = lens[OrderPayment].id
)(new OrderPayments(_)){

  def findAllByOrderId(id: Int): QuerySeq =
    filter(_.orderId === id)

  def findAllStoreCredit: QuerySeq =
    filter(_.paymentMethodType === (PaymentMethod.StoreCredit: PaymentMethod.Type))

  def findAllGiftCardsByOrderId(id: Int): Query[(OrderPayments, GiftCards), (OrderPayment, GiftCard), Seq] =
    for {
      pmts  ← OrderPayments.filter(_.orderId === id)
      gc    ← GiftCards if gc.id === pmts.paymentMethodId
    } yield (pmts, gc)

  def findAllStoreCreditsByOrderId(id: Int): Query[(OrderPayments, StoreCredits), (OrderPayment, StoreCredit), Seq] =
    for {
      pmts  ← OrderPayments.filter(_.orderId === id)
      sc    ← StoreCredits if sc.id === pmts.paymentMethodId
    } yield (pmts, sc)

  def findAllCreditCardsForOrder(orderId: Rep[Int]): QuerySeq =
    filter(_.orderId === orderId).filter(_.paymentMethodType === (PaymentMethod.CreditCard: PaymentMethod.Type))

  object scope {
    implicit class OrderPaymentsQuerySeqConversions(q: QuerySeq) {
      def giftCards:    QuerySeq = q.byType(PaymentMethod.GiftCard)
      def creditCards:  QuerySeq = q.byType(PaymentMethod.CreditCard)
      def storeCredits: QuerySeq = q.byType(PaymentMethod.StoreCredit)

      def byType(pmt: PaymentMethod.Type): QuerySeq = q.filter(_.paymentMethodType === (pmt: PaymentMethod.Type))
    }
  }
}
