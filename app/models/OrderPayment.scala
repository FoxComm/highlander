package models

import cats.data.ValidatedNel
import cats.data.Validated.valid
import com.stripe.model.{Customer ⇒ StripeCustomer}
import monocle.macros.GenLens
import services.Failure
import slick.driver.PostgresDriver.api._
import utils.{TableQueryWithId, GenericTable, ModelWithIdParameter}
import utils.Money._
import utils.Slick.implicits._
import utils.Validation._

final case class OrderPayment(id: Int = 0, orderId: Int = 0, amount: Option[Int] = None,
  currency: Currency = Currency.USD, paymentMethodId: Int, paymentMethodType: PaymentMethod.Type)
  extends ModelWithIdParameter[OrderPayment] {

  def isCreditCard:   Boolean = paymentMethodType == PaymentMethod.CreditCard
  def isGiftCard:     Boolean = paymentMethodType == PaymentMethod.GiftCard
  def isStoreCredit:  Boolean = paymentMethodType == PaymentMethod.StoreCredit

  override def validate: ValidatedNel[Failure, OrderPayment] = {
    val amountOk = paymentMethodType match {
      case PaymentMethod.StoreCredit | PaymentMethod.GiftCard ⇒
        invalidExpr(amount.getOrElse(0) > 0, s"amount must be > 0 for ${paymentMethodType}")
      case PaymentMethod.CreditCard ⇒
        invalidExpr(amount.isDefined, "amount must be empty for creditCard")
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

class OrderPayments(tag: Tag)
  extends GenericTable.TableWithId[OrderPayment](tag, "order_payments")
   {

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

object OrderPayments extends TableQueryWithId[OrderPayment, OrderPayments](
  idLens = GenLens[OrderPayment](_.id)
)(new OrderPayments(_)){

  import models.{PaymentMethod ⇒ Pay}

  def findAllByOrderId(id: Int): QuerySeq =
    filter(_.orderId === id)

  def findAllStoreCredit: QuerySeq =
    filter(_.paymentMethodType === (Pay.StoreCredit: Pay.Type))

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
    filter(_.orderId === orderId).filter(_.paymentMethodType === (Pay.CreditCard: Pay.Type))

  object scope {
    implicit class OrderPaymentsQuerySeqConversions(q: QuerySeq) {
      def giftCards:    QuerySeq = q.byType(Pay.GiftCard)
      def creditCards:  QuerySeq = q.byType(Pay.CreditCard)
      def storeCredits: QuerySeq = q.byType(Pay.StoreCredit)

      def byType(pmt: Pay.Type): QuerySeq = filter(_.paymentMethodType === (pmt: Pay.Type))
    }
  }
}
