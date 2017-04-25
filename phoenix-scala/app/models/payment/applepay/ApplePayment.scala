package models.payment.applepay

import java.time.Instant

import models.payment.PaymentMethod
import models.payment.creditcard.BillingAddress
import shapeless.{Lens, lens}
import slick.driver.PostgresDriver.api._
import utils.db._

case class ApplePayment(
    id: Int = 0,
    accountId: Int,
    stripeTokenId: String,
    stripeCustomerId: Option[String] = None, // will be necessary for recurring payments only
    deletedAt: Option[Instant] = None,
    createdAt: Instant = Instant.now())
    extends PaymentMethod
    with FoxModel[ApplePayment] {

  // todo validate stripeTokenId should start with "tok_"
}

class ApplePayments(tag: Tag) extends FoxTable[ApplePayment](tag, "apple_payments") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId        = column[Int]("account_id")
  def stripeTokenId    = column[String]("stripe_token_id")
  def stripeCustomerId = column[Option[String]]("stripe_customer_id")
  def deletedAt        = column[Option[Instant]]("deleted_at")
  def createdAt        = column[Instant]("created_at")

  def * =
    (id, accountId, stripeTokenId, stripeCustomerId, deletedAt, createdAt) <> ((ApplePayment.apply _).tupled, ApplePayment.unapply)
}

object ApplePayments
    extends FoxTableQuery[ApplePayment, ApplePayments](new ApplePayments(_))
    with ReturningId[ApplePayment, ApplePayments] {

  val returningLens: Lens[ApplePayment, Int] = lens[ApplePayment].id
}
