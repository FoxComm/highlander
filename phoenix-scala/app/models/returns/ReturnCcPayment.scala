package models.returns

import models.payment.creditcard.CreditCardCharges
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class ReturnCcPayment(id: Int = 0,
                           returnPaymentId: Int,
                           chargeId: String,
                           returnId: Int,
                           amount: Int)
    extends FoxModel[ReturnCcPayment]

object ReturnCcPayment {
  def build(returnPaymentId: Int, chargeId: String, returnId: Int, amount: Int): ReturnCcPayment =
    ReturnCcPayment(
        returnPaymentId = returnPaymentId,
        chargeId = chargeId,
        returnId = returnId,
        amount = amount
    )
}

class ReturnCcPayments(tag: Tag) extends FoxTable[ReturnCcPayment](tag, "return_cc_payments") {
  def id              = column[Int]("id", O.AutoInc)
  def returnPaymentId = column[Int]("return_payment_id")
  def chargeId        = column[String]("charge_id")
  def returnId        = column[Int]("return_id")
  def amount          = column[Int]("amount")

  def * =
    (id, returnPaymentId, chargeId, returnId, amount) <> ((ReturnCcPayment.apply _).tupled,
        ReturnCcPayment.unapply)

  def pk = primaryKey(tableName, (returnPaymentId, chargeId))
  def creditCardCharge =
    foreignKey(CreditCardCharges.tableName, chargeId, CreditCardCharges)(_.chargeId)
  def returnPayment = foreignKey(ReturnPayments.tableName, returnPaymentId, ReturnPayments)(_.id)
  def rma           = foreignKey(Returns.tableName, returnId, Returns)(_.id)
}

object ReturnCcPayments
    extends FoxTableQuery[ReturnCcPayment, ReturnCcPayments](new ReturnCcPayments(_))
    with ReturningId[ReturnCcPayment, ReturnCcPayments] {

  val returningLens: Lens[ReturnCcPayment, Int] = lens[ReturnCcPayment].id
}
