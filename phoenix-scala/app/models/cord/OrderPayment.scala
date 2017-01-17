package models.cord

import cats.data.ValidatedNel
import failures.Failure
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCard, CreditCards}
import models.payment.giftcard.{GiftCard, GiftCards}
import models.payment.storecredit.{StoreCredit, StoreCredits}
import shapeless._
import utils.Money._
import utils.Validation._
import utils.aliases.stripe._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class OrderPayment(id: Int = 0,
                        // FIXME @anna WTF is wrong with these defaults?
                        // CURRY ALL THE THINGS
                        cordRef: String = "",
                        amount: Option[Int] = None,
                        currency: Currency = Currency.USD,
                        paymentMethodId: Int,
                        paymentMethodType: PaymentMethod.Type)
    extends FoxModel[OrderPayment] {

  def isCreditCard: Boolean  = paymentMethodType == PaymentMethod.CreditCard
  def isGiftCard: Boolean    = paymentMethodType == PaymentMethod.GiftCard
  def isStoreCredit: Boolean = paymentMethodType == PaymentMethod.StoreCredit

  override def validate: ValidatedNel[Failure, OrderPayment] = {
    val amountOk = paymentMethodType match {
      case PaymentMethod.StoreCredit | PaymentMethod.GiftCard ⇒
        validExpr(amount.getOrElse(0) > 0, s"amount must be > 0 for $paymentMethodType")
      case PaymentMethod.CreditCard ⇒
        validExpr(amount.isEmpty, "amount must be empty for creditCard")
    }

    amountOk.map(_ ⇒ this)
  }

  def getAmount(amountLimit: Option[Int] = None) = {
    val paymentAmount = amount.getOrElse(0)
    amountLimit.fold(paymentAmount)(_.min(paymentAmount))
  }
}

object OrderPayment {
  def fromStripeCustomer(stripeCustomer: StripeCustomer, cart: Cart): OrderPayment =
    OrderPayment(cordRef = cart.refNum,
                 paymentMethodId = 1,
                 paymentMethodType = PaymentMethod.CreditCard)

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

  def id                = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cordRef           = column[String]("cord_ref")
  def paymentMethodId   = column[Int]("payment_method_id")
  def paymentMethodType = column[PaymentMethod.Type]("payment_method_type")
  def amount            = column[Option[Int]]("amount")
  def currency          = column[Currency]("currency")

  def * =
    (id, cordRef, amount, currency, paymentMethodId, paymentMethodType) <> ((OrderPayment.apply _).tupled,
    OrderPayment.unapply)

  def order      = foreignKey(Carts.tableName, cordRef, Carts)(_.referenceNumber)
  def creditCard = foreignKey(CreditCards.tableName, paymentMethodId, CreditCards)(_.id)
}

object OrderPayments
    extends FoxTableQuery[OrderPayment, OrderPayments](new OrderPayments(_))
    with ReturningId[OrderPayment, OrderPayments] {

  val returningLens: Lens[OrderPayment, Int] = lens[OrderPayment].id

  def findAllByCordRef(cordRef: String): QuerySeq =
    filter(_.cordRef === cordRef)

  def findAllStoreCredit: QuerySeq =
    filter(_.paymentMethodType === (PaymentMethod.StoreCredit: PaymentMethod.Type))

  def findAllGiftCardsByCordRef(
      cordRef: String): Query[(OrderPayments, GiftCards), (OrderPayment, GiftCard), Seq] =
    for {
      pmts ← OrderPayments.filter(_.cordRef === cordRef)
      gc   ← GiftCards if gc.id === pmts.paymentMethodId
    } yield (pmts, gc)

  def findAllStoreCreditsByCordRef(
      cordRef: String): Query[(OrderPayments, StoreCredits), (OrderPayment, StoreCredit), Seq] =
    for {
      pmts ← OrderPayments.filter(_.cordRef === cordRef)
      sc   ← StoreCredits if sc.id === pmts.paymentMethodId
    } yield (pmts, sc)

  def findAllCreditCardsForOrder(cordRef: Rep[String]): QuerySeq =
    filter(_.cordRef === cordRef)
      .filter(_.paymentMethodType === (PaymentMethod.CreditCard: PaymentMethod.Type))

  object scope {
    implicit class OrderPaymentsQuerySeqConversions(q: QuerySeq) {
      def giftCards: QuerySeq    = q.byType(PaymentMethod.GiftCard)
      def creditCards: QuerySeq  = q.byType(PaymentMethod.CreditCard)
      def storeCredits: QuerySeq = q.byType(PaymentMethod.StoreCredit)

      def byType(pmt: PaymentMethod.Type): QuerySeq =
        q.filter(_.paymentMethodType === (pmt: PaymentMethod.Type))

      def inStoreMethods: QuerySeq =
        q.filter(_.paymentMethodType.inSet(Seq(PaymentMethod.GiftCard, PaymentMethod.StoreCredit)))

      def byCartAndGiftCard(cart: Cart, giftCard: GiftCard): QuerySeq =
        q.giftCards.filter(_.paymentMethodId === giftCard.id).filter(_.cordRef === cart.refNum)
    }
  }
}
