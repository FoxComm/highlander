package models

import monocle.macros.GenLens
import utils._
import payloads.CreateAddressPayload

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}
import com.stripe.model.{Customer => StripeCustomer}

final case class OrderPayment(id: Int = 0,
                          orderId: Int = 0,
                          paymentMethodId: Int,
                          paymentMethodType: String,
                          appliedAmount: Int,
                          status: String,
                          responseCode: String,
                          chargeId: Option[String] = None)
  extends ModelWithIdParameter {
}

object OrderPayment {
  def fromStripeCustomer(stripeCustomer: StripeCustomer, order: Order): OrderPayment = {
    OrderPayment(orderId = order.id, paymentMethodId = 1, // TODO: would do a lookup
      paymentMethodType = "stripe",
      appliedAmount = 0, status = Auth.toString.toLowerCase, // TODO: use type and marshalling
      responseCode = "ok" // TODO: make this real
    )
  }
}

class OrderPayments(tag: Tag)
  extends GenericTable.TableWithId[OrderPayment](tag, "order_payments")
  with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def paymentMethodId = column[Int]("payment_method_id")
  def paymentMethodType = column[String]("payment_method_type")
  def amount = column[Int]("amount")
  def status = column[String]("status")
  def responseCode = column[String]("response_code")
  def chargeId = column[Option[String]]("charge_id")

  def * = (id, orderId, paymentMethodId, paymentMethodType, amount, status, responseCode, chargeId) <> ((OrderPayment.apply _).tupled, OrderPayment.unapply )
}

object OrderPayments extends TableQueryWithId[OrderPayment, OrderPayments](
  idLens = GenLens[OrderPayment](_.id)
)(new OrderPayments(_)){

  def update(payment: OrderPayment)(implicit db: Database): Future[Int] =
    this._findById(payment.id).update(payment).run()

  def findAllByOrderId(id: Int)(implicit ec: ExecutionContext, db: Database): Future[Seq[OrderPayment]] = {
    db.run(this.filter(_.id === id).result)
  }

  def findAllPaymentsFor(order: Order)
                        (implicit ec: ExecutionContext, db: Database): Future[Seq[(OrderPayment, CreditCard)]] = {
    db.run(this._findAllPaymentsFor(order).result)
  }

  def _findAllPaymentsFor(order: Order): Query[(OrderPayments, CreditCards), (OrderPayment, CreditCard), Seq] = {
    for {
      payments ← this.filter(_.orderId === order.id)
      cards    ← CreditCards if cards.id === payments.id
    } yield (payments, cards)
  }
}
