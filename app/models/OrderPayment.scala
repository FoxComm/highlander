package models

import scala.concurrent.{ExecutionContext, Future}

import com.stripe.model.{Customer ⇒ StripeCustomer}
import models.PaymentMethods._
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.Money._
import utils._

final case class OrderPayment(id: Int = 0, orderId: Int = 0, amount: Option[Int] = None,
  currency: Currency = Currency.USD, paymentMethodId: Int, paymentMethodType: PaymentMethods.Type)
  extends ModelWithIdParameter

object OrderPayment {
  def fromStripeCustomer(stripeCustomer: StripeCustomer, order: Order): OrderPayment =
    OrderPayment(orderId = order.id, paymentMethodId = 1, paymentMethodType = PaymentMethods.CreditCard)

  def build(method: PaymentMethod): OrderPayment = method match {
    case gc: GiftCard ⇒
      OrderPayment(paymentMethodId = gc.id, paymentMethodType = PaymentMethods.GiftCard)
    case cc: CreditCard ⇒
      OrderPayment(paymentMethodId = cc.id, paymentMethodType = PaymentMethods.CreditCard)
    case sc: StoreCredit ⇒
      OrderPayment(paymentMethodId = sc.id, paymentMethodType = PaymentMethods.StoreCredit)
  }
}

class OrderPayments(tag: Tag)
  extends GenericTable.TableWithId[OrderPayment](tag, "order_payments")
  with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def paymentMethodId = column[Int]("payment_method_id")
  def paymentMethodType = column[PaymentMethods.Type]("payment_method_type")
  def amount = column[Option[Int]]("amount")
  def currency = column[Currency]("currency")

  def * = (id, orderId, amount, currency, paymentMethodId, paymentMethodType) <> ((OrderPayment.apply _).tupled,
    OrderPayment.unapply )
}

object OrderPayments extends TableQueryWithId[OrderPayment, OrderPayments](
  idLens = GenLens[OrderPayment](_.id)
)(new OrderPayments(_)){

  def update(payment: OrderPayment)(implicit db: Database): Future[Int] =
    this._findById(payment.id).update(payment).run()

  def findAllByOrderId(id: Int): Query[OrderPayments, OrderPayment, Seq] =
    filter(_.id === id)

  def findAllPaymentsFor(order: Order)
                        (implicit ec: ExecutionContext, db: Database): Future[Seq[(OrderPayment, CreditCard)]] = {
    db.run(this._findAllPaymentsFor(order.id).result)
  }

  def _findAllPaymentsFor(orderId: Int): Query[(OrderPayments, CreditCards), (OrderPayment, CreditCard), Seq] = {
    for {
      payments ← this.filter(_.orderId === orderId)
      cards    ← CreditCards if cards.id === payments.id
    } yield (payments, cards)
  }
}
