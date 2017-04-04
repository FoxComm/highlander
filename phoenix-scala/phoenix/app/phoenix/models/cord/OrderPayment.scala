package phoenix.models.cord

import cats.data.ValidatedNel
import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.Failure
import core.utils.Money._
import core.utils.Validation._
import phoenix.models.payment.PaymentMethod
import phoenix.models.payment.creditcard.{CreditCard, CreditCards}
import phoenix.models.payment.applepay.{ApplePayment, ApplePayments}
import phoenix.models.payment.giftcard.{GiftCard, GiftCards}
import phoenix.models.payment.storecredit.{StoreCredit, StoreCredits}
import phoenix.utils.aliases.stripe.StripeCustomer
import shapeless._

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
  def isApplePay: Boolean    = paymentMethodType == PaymentMethod.ApplePay

  override def validate: ValidatedNel[Failure, OrderPayment] = {
    val amountOk = paymentMethodType match {
      case PaymentMethod.StoreCredit | PaymentMethod.GiftCard | PaymentMethod.ApplePay ⇒
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
    case ap: ApplePayment ⇒
      OrderPayment(paymentMethodId = ap.id, paymentMethodType = PaymentMethod.ApplePay)
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
  import scope._

  val returningLens: Lens[OrderPayment, Int] = lens[OrderPayment].id

  def findAllByCordRef(cordRef: String): QuerySeq =
    filter(_.cordRef === cordRef)

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

  def findAllApplePayByCordRef(
      cordRef: String): Query[(OrderPayments, ApplePayments), (OrderPayment, ApplePayment), Seq] =
    for {
      pmts ← OrderPayments.filter(_.cordRef === cordRef)
      ap   ← ApplePayments if ap.id === pmts.paymentMethodId
    } yield (pmts, ap)

  def findAllCreditCardsForOrder(cordRef: Rep[String]): QuerySeq =
    filter(_.cordRef === cordRef).creditCards

  object scope {
    implicit class OrderPaymentsQuerySeqConversions(q: QuerySeq) {
      def giftCards: QuerySeq    = q.byType(PaymentMethod.GiftCard)
      def creditCards: QuerySeq  = q.byType(PaymentMethod.CreditCard)
      def storeCredits: QuerySeq = q.byType(PaymentMethod.StoreCredit)
      def applePays: QuerySeq    = q.byType(PaymentMethod.ApplePay)

      def byType(pmt: PaymentMethod.Type): QuerySeq =
        q.filter(_.paymentMethodType === (pmt: PaymentMethod.Type))

      def inStoreMethods: QuerySeq =
        q.filter(_.paymentMethodType.inSet(Seq(PaymentMethod.GiftCard, PaymentMethod.StoreCredit)))

      def byCartAndGiftCard(cart: Cart, giftCard: GiftCard): QuerySeq =
        q.giftCards.filter(_.paymentMethodId === giftCard.id).filter(_.cordRef === cart.refNum)
    }
  }
}
