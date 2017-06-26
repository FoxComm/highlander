package phoenix.models.cord

import cats.data.ValidatedNel
import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.Failure
import core.utils.Money._
import core.utils.Validation._
import core.failures.Failure
import phoenix.models.cord.OrderPayments.filter
import phoenix.models.payment.PaymentMethod
import phoenix.models.payment.PaymentMethod.ExternalPayment
import phoenix.models.payment.creditcard.{CreditCard, CreditCardCharges, CreditCards}
import phoenix.models.payment.applepay.{ApplePayCharge, ApplePayCharges, ApplePayment, ApplePayments}
import phoenix.models.payment.giftcard.{GiftCard, GiftCards}
import phoenix.models.payment.storecredit.{StoreCredit, StoreCredits}
import phoenix.utils.aliases.stripe.StripeCustomer
import shapeless._
import core.db.ExPostgresDriver.api._
import core.db._

case class OrderPayment(id: Int = 0,
                        // FIXME @anna WTF is wrong with these defaults?
                        // CURRY ALL THE THINGS
                        cordRef: String = "",
                        amount: Option[Long] = None,
                        currency: Currency = Currency.USD,
                        paymentMethodId: Int,
                        paymentMethodType: PaymentMethod.Type)
    extends FoxModel[OrderPayment] {

  def isCreditCard: Boolean  = paymentMethodType == PaymentMethod.CreditCard
  def isGiftCard: Boolean    = paymentMethodType == PaymentMethod.GiftCard
  def isStoreCredit: Boolean = paymentMethodType == PaymentMethod.StoreCredit
  def isApplePay: Boolean    = paymentMethodType == PaymentMethod.ApplePay

  def isExternalFunds: Boolean = paymentMethodType.isExternal

  override def validate: ValidatedNel[Failure, OrderPayment] = {
    val amountOk = paymentMethodType match {
      case t if PaymentMethod.Type.internalPayments.contains(t) ⇒
        validExpr(amount.getOrElse(0L) > 0, s"amount must be > 0 for $paymentMethodType")
      case t if PaymentMethod.Type.externalPayments.contains(t) ⇒
        validExpr(amount.isEmpty, "amount must be empty for ExternalFunds")
    }

    amountOk.map(_ ⇒ this)
  }

  def getAmount(amountLimit: Option[Long] = None): Long = {
    val paymentAmount: Long = amount.getOrElse(0)
    amountLimit.fold(paymentAmount)(_.min(paymentAmount))
  }
}

case class StripeOrderPayment(stripeChargeId: String, amount: Long, currency: Currency = Currency.USD)

object OrderPayment {
  def fromStripeCustomer(stripeCustomer: StripeCustomer, cart: Cart): OrderPayment =
    OrderPayment(cordRef = cart.refNum, paymentMethodId = 1, paymentMethodType = PaymentMethod.CreditCard)

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
  def amount            = column[Option[Long]]("amount")
  def currency          = column[Currency]("currency")

  def * =
    (id, cordRef, amount, currency, paymentMethodId, paymentMethodType) <> ((OrderPayment.apply _).tupled,
    OrderPayment.unapply)

  def order        = foreignKey(Carts.tableName, cordRef, Carts)(_.referenceNumber)
  def creditCard   = foreignKey(CreditCards.tableName, paymentMethodId, CreditCards)(_.id)
  def applePayment = foreignKey(ApplePayments.tableName, paymentMethodId, ApplePayments)(_.id)
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

  def findAllApplePaysByCordRef(
      cordRef: String): Query[(OrderPayments, ApplePayments), (OrderPayment, ApplePayment), Seq] =
    for {
      pmts ← OrderPayments.filter(_.cordRef === cordRef)
      ap   ← ApplePayments if ap.id === pmts.paymentMethodId
    } yield (pmts, ap)

  def applePayByCordRef(cordRef: String): QuerySeq =
    findAllByCordRef(cordRef).applePays

  def findAllCreditCardsForOrder(cordRef: String): QuerySeq =
    findAllByCordRef(cordRef).creditCards

  def findAllExternalPayments(cordRef: String): QuerySeq =
    findAllByCordRef(cordRef).externalPayments

  def findAllStripeCharges(cordRef: Rep[String]): Query[Rep[StripeOrderPayment], StripeOrderPayment, Seq] = {
    def ccCharges =
      filter(_.cordRef === cordRef).join(CreditCardCharges).on(_.paymentMethodId === _.id).map {
        case (_, charge) ⇒
          (charge.stripeChargeId, charge.amount, charge.currency) <> (StripeOrderPayment.tupled, StripeOrderPayment.unapply _)
      }

    def applePayCharges =
      filter(_.cordRef === cordRef).join(ApplePayCharges).on(_.paymentMethodId === _.id).map {
        case (_, charge) ⇒
          (charge.stripeChargeId, charge.amount, charge.currency) <> (StripeOrderPayment.tupled, StripeOrderPayment.unapply _)
      }

    ccCharges ++ applePayCharges
  }

  object scope {
    implicit class OrderPaymentsQuerySeqConversions(q: QuerySeq) {
      def giftCards: QuerySeq    = q.byType(PaymentMethod.GiftCard)
      def creditCards: QuerySeq  = q.byType(PaymentMethod.CreditCard)
      def storeCredits: QuerySeq = q.byType(PaymentMethod.StoreCredit)
      def applePays: QuerySeq    = q.byType(PaymentMethod.ApplePay)

      def externalPayments: QuerySeq =
        q.filter(_.paymentMethodType.inSet(PaymentMethod.Type.externalPayments))

      def byType(pmt: PaymentMethod.Type): QuerySeq =
        q.filter(_.paymentMethodType === (pmt: PaymentMethod.Type))

      def inStoreMethods: QuerySeq =
        q.filter(_.paymentMethodType.inSet(PaymentMethod.Type.internalPayments))

      def byCartAndGiftCard(cart: Cart, giftCard: GiftCard): QuerySeq =
        q.giftCards.filter(_.paymentMethodId === giftCard.id).filter(_.cordRef === cart.refNum)
    }
  }
}
