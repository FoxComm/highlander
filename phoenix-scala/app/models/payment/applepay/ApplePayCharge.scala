package models.payment.applepay

import java.time.Instant

import akka.actor.FSM
import slick.driver.PostgresDriver.api._
import models.payment.{PaymentMethod, applepay}
import models.payment.applepay.ApplePayCharge.State
import models.payment.creditcard.BillingAddress
import shapeless.Lens
import slick.lifted._
import utils.Money.Currency
import utils.db._

case class ApplePayCharge(id: Int = 0,
                          accountId: Int,
                          gatewayCustomerId: String,
                          currency: Currency = Currency.USD,
                          amount: Int,
                          deletedAt: Option[Instant] = None,
                          createdAt: Instant = Instant.now())
    extends FoxModel[ApplePayCharge]
    with FSM[ApplePayCharge.State, ApplePayCharge] {}

//TODO add state field
object ApplePayCharge {
  sealed trait State
  object STATUS_SUCCESS extends State
  object STATUS_FAILURE extends State
}

class ApplePayCharges(tag: Tag) extends FoxTable[ApplePayCharge](tag, "apple_pay_charges") {
  def id                = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId         = column[Int]("account_id")
  def gatewayCustomerId = column[String]("gateway_customer_id")
  def currency          = column[Currency]("currency")
  def amount            = column[Int]("amount")
  def deletedAt         = column[Option[Instant]]("deleted_at")
  def createdAt         = column[Instant]("created_at")

  def * =
    (id, accountId, gatewayCustomerId, currency, amount, deletedAt, createdAt) <> ((ApplePayCharge.apply _).tupled, ApplePayCharge.unapply)
}

object ApplePayCharges
    extends FoxTableQuery[ApplePayCharge, ApplePayCharges](new ApplePayCharges(_))
    with ReturningId[ApplePayCharge, ApplePayCharges] {
  override val returningQuery = ???
  override val returningLens  = ???
}
