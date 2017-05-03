package models.returns

import models.payment.creditcard.CreditCardCharges
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.Money.Currency
import utils.db._

case class ReturnStripePayment(id: Int = 0,
                               returnPaymentId: Int,
                               chargeId: String,
                               returnId: Int,
                               amount: Int,
                               currency: Currency)
    extends FoxModel[ReturnStripePayment]

class ReturnStripePayments(tag: Tag)
    extends FoxTable[ReturnStripePayment](tag, "return_stripe_payments") {
  def id              = column[Int]("id", O.AutoInc)
  def returnPaymentId = column[Int]("return_payment_id")
  def chargeId        = column[String]("charge_id")
  def returnId        = column[Int]("return_id")
  def amount          = column[Int]("amount")
  def currency        = column[Currency]("currency")

  def * =
    (id, returnPaymentId, chargeId, returnId, amount, currency) <> ((ReturnStripePayment.apply _).tupled,
        ReturnStripePayment.unapply)

  def pk = primaryKey(tableName, (returnPaymentId, chargeId))
  def creditCardCharge =
    foreignKey(CreditCardCharges.tableName, chargeId, CreditCardCharges)(_.stripeChargeId)
  def returnPayment = foreignKey(ReturnPayments.tableName, returnPaymentId, ReturnPayments)(_.id)
  def rma           = foreignKey(Returns.tableName, returnId, Returns)(_.id)
}

object ReturnStripePayments
    extends FoxTableQuery[ReturnStripePayment, ReturnStripePayments](new ReturnStripePayments(_))
    with ReturningId[ReturnStripePayment, ReturnStripePayments] {

  val returningLens: Lens[ReturnStripePayment, Int] = lens[ReturnStripePayment].id
}
