package models.payment.applepay

import java.time.Instant

import models.payment.PaymentMethod
import models.payment.creditcard.BillingAddress
import slick.driver.PostgresDriver.api._
import utils.db._

// FIXME do we need to keep this details?
case class ApplePayment(id: Int = 0,
                        accountId: Int,
                        gatewayCustomerId: String,
                        deletedAt: Option[Instant] = None,
                        createdAt: Instant = Instant.now())
    extends PaymentMethod
    with FoxModel[ApplePayment] {}

object ApplePayment {}

class ApplePayments(tag: Tag) extends FoxTable[ApplePayment](tag, "apple_payments") {
  def id                = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId         = column[Int]("accountId")
  def gatewayCustomerId = column[String]("gatewayCustomerId")
  def deletedAt         = column[Option[Instant]]("deletedAt")
  def createdAt         = column[Instant]("createdAt")

  def * =
    (id, accountId, gatewayCustomerId, deletedAt, createdAt) <> ((ApplePayment.apply _).tupled, ApplePayment.unapply)
}

object ApplePayments extends FoxTableQuery[ApplePayment, ApplePayments](new ApplePayments(_)) {
  def authOrderPayment = ???

  override type Ret       = this.type
  override type PackedRet = this.type
  override val returningQuery = ???
  override val returningLens  = ???
}
