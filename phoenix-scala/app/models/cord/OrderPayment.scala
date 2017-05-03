package models.cord

import cats.data.ValidatedNel
import failures.Failure
import models.payment.PaymentMethod
import models.payment.PaymentMethod.ExternalPayment
import models.payment.applepay.{ApplePayCharges, ApplePayment, ApplePayments}
import models.payment.creditcard.{CreditCard, CreditCardCharges, CreditCards}
import models.payment.giftcard.{GiftCard, GiftCards}
import models.payment.storecredit.{StoreCredit, StoreCredits}
import shapeless._
import slick.jdbc.GetResult
import slick.lifted.{Rep, RepOption, ShapedValue}
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
  def isApplePay: Boolean    = paymentMethodType == PaymentMethod.ApplePay

  def isExternalFunds: Boolean = paymentMethodType.isExternal

  override def validate: ValidatedNel[Failure, OrderPayment] = {
    val amountOk = paymentMethodType match {
      case t if PaymentMethod.Type.internalPayments.contains(t) ⇒
        validExpr(amount.getOrElse(0) > 0, s"amount must be > 0 for $paymentMethodType")
      case t if PaymentMethod.Type.externalPayments.contains(t) ⇒
        validExpr(amount.isEmpty, "amount must be empty for ExternalFunds")
    }

    amountOk.map(_ ⇒ this)
  }

  def getAmount(amountLimit: Option[Int] = None) = {
    val paymentAmount = amount.getOrElse(0)
    amountLimit.fold(paymentAmount)(_.min(paymentAmount))
  }
}

case class StripeOrderPayment(stripeChargeId: String,
                              amount: Int,
                              currency: Currency = Currency.USD)

object OrderPayment {

  // it is used in tests only
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

  def applePayByCordRef(cordRef: String): QuerySeq =
    filter(_.cordRef === cordRef).applePays

  def findAllCreditCardsForOrder(cordRef: Rep[String]): QuerySeq =
    filter(_.cordRef === cordRef).creditCards

  def findAllExternalPayments(cordRef: Rep[String]): QuerySeq =
    filter(_.cordRef === cordRef).externalPayments

  def findAllStripeCharges(
      cordRef: Rep[String]): Query[Rep[StripeOrderPayment], StripeOrderPayment, Seq] = {
    def ccCharges = {
      filter(_.cordRef === cordRef).join(CreditCardCharges).on(_.paymentMethodId === _.id).map {
        case (_, charge) ⇒
          ((charge.stripeChargeId, charge.amount, charge.currency) <> (StripeOrderPayment.tupled, StripeOrderPayment.unapply _))
      }
    }

    def applePayCharges = {
      filter(_.cordRef === cordRef).join(ApplePayCharges).on(_.paymentMethodId === _.id).map {
        case (_, charge) ⇒
          ((charge.stripeChargeId, charge.amount, charge.currency) <> (StripeOrderPayment.tupled, StripeOrderPayment.unapply _))
      }
    }

    ccCharges.unionAll(applePayCharges)
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
