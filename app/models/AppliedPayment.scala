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

case class AppliedPayment(id: Int = 0,
                          orderId: Int,
                          paymentMethodId: Int,
                          paymentMethodType: String,
                          appliedAmount: Int,
                          status: String,
                          responseCode: String)
  extends ModelWithIdParameter {
}

object AppliedPayment {
  def fromStripeCustomer(stripeCustomer: StripeCustomer, order: Order): AppliedPayment = {
    AppliedPayment(orderId = order.id, paymentMethodId = 1, // TODO: would do a lookup
      paymentMethodType = "stripe",
      appliedAmount = 0, status = Auth.toString.toLowerCase, // TODO: use type and marshalling
      responseCode = "ok" // TODO: make this real
    )
  }
}

class AppliedPayments(tag: Tag)
  extends GenericTable.TableWithId[AppliedPayment](tag, "applied_payments")
  with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def paymentMethodId = column[Int]("payment_method_id")
  def paymentMethodType = column[String]("payment_method_type")
  def appliedAmount = column[Int]("applied_amount")
  def status = column[String]("status")
  def responseCode = column[String]("response_code")
  def * = (id, orderId, paymentMethodId, paymentMethodType, appliedAmount, status, responseCode) <> ((AppliedPayment.apply _).tupled, AppliedPayment.unapply )
}

object AppliedPayments extends TableQueryWithId[AppliedPayment, AppliedPayments](
  idLens = GenLens[AppliedPayment](_.id)
)(new AppliedPayments(_)){

  def findAllByOrderId(id: Int)(implicit ec: ExecutionContext, db: Database): Future[Seq[AppliedPayment]] = {
    db.run(this.filter(_.id === id).result)
  }

  def findAllPaymentsFor(order: Order)
                        (implicit ec: ExecutionContext, db: Database): Future[Seq[(AppliedPayment, CreditCardGateway)]] = {
    db.run(this._findAllPaymentsFor(order).result)
  }

  def _findAllPaymentsFor(order: Order) = {
    for {
      result <- this.filter(_.orderId === order.id).join(CreditCardGateways).on(_.id === _.id)
    } yield result
  }
}
