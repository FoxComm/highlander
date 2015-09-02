package models

import scala.concurrent.{ExecutionContext, Future}

import com.stripe.model.{Customer ⇒ StripeCustomer}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.Money._
import utils._

final case class OrderPayment(id: Int = 0, orderId: Int = 0, amount: Option[Int] = None,
  currency: Currency = Currency.USD, paymentMethodId: Int, paymentMethodType: PaymentMethod.Type)
  extends ModelWithIdParameter

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
  with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def paymentMethodId = column[Int]("payment_method_id")
  def paymentMethodType = column[PaymentMethod.Type]("payment_method_type")
  def amount = column[Option[Int]]("amount")
  def currency = column[Currency]("currency")

  def * = (id, orderId, amount, currency, paymentMethodId, paymentMethodType) <> ((OrderPayment.apply _).tupled,
    OrderPayment.unapply )
}

object OrderPayments extends TableQueryWithId[OrderPayment, OrderPayments](
  idLens = GenLens[OrderPayment](_.id)
)(new OrderPayments(_)){

  import models.{PaymentMethod ⇒ Pay}

  def update(payment: OrderPayment)(implicit db: Database): Future[Int] =
    this._findById(payment.id).update(payment).run()

  def findAllByOrderId(id: Int): Query[OrderPayments, OrderPayment, Seq] =
    filter(_.orderId === id)

  def findAllPaymentsFor(order: Order)
                        (implicit ec: ExecutionContext, db: Database): Future[Seq[(OrderPayment, CreditCard)]] = {
    db.run(this._findAllPaymentsFor(order.id).result)
  }

  def findAllStoreCredit: Query[OrderPayments, OrderPayment, Seq] =
    filter(_.paymentMethodType === (Pay.StoreCredit: Pay.Type))

  def _findAllPaymentsFor(orderId: Int): Query[(OrderPayments, CreditCards), (OrderPayment, CreditCard), Seq] = {
    for {
      payments ← this.filter(_.orderId === orderId)
      cards    ← CreditCards if cards.id === payments.id
    } yield (payments, cards)
  }

  def findAllCreditCardsForOrder(orderId: Rep[Int]): Query[OrderPayments, OrderPayment, Seq] =
    filter(_.orderId === orderId).filter(_.paymentMethodType === (Pay.CreditCard: Pay.Type))

  def byType(pmt: Pay.Type): Query[OrderPayments, OrderPayment, Seq] =
    filter(_.paymentMethodType === (pmt: Pay.Type))

  def giftCards: Query[OrderPayments, OrderPayment, Seq]    = byType(Pay.GiftCard)

  def creditCards: Query[OrderPayments, OrderPayment, Seq]  = byType(Pay.CreditCard)

  def storeCredits: Query[OrderPayments, OrderPayment, Seq] = byType(Pay.StoreCredit)
}
