package models.payment.applepay

import java.time.Instant

import models.cord.OrderPayment
import models.payment.ExternalCharge
import models.payment.ExternalCharge._
import shapeless.{Lens, lens}
import slick.jdbc.PostgresProfile.api._
import slick.lifted._
import utils.Money.Currency
import utils.aliases.stripe.StripeCharge
import utils.db._

case class ApplePayCharge(id: Int = 0,
                          orderPaymentId: Int,
                          stripeChargeId: String,
                          state: State = Cart,
                          currency: Currency = Currency.USD,
                          amount: Int = 0,
                          deletedAt: Option[Instant] = None,
                          createdAt: Instant = Instant.now())
    extends ExternalCharge[ApplePayCharge] {

  def stateLens = lens[ApplePayCharge].state
}

class ApplePayCharges(tag: Tag) extends FoxTable[ApplePayCharge](tag, "apple_pay_charges") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderPaymentId = column[Int]("order_payment_id")
  def stripeChargeId = column[String]("stripe_charge_id")
  def state          = column[State]("state")
  def currency       = column[Currency]("currency")
  def amount         = column[Int]("amount")
  def deletedAt      = column[Option[Instant]]("deleted_at")
  def createdAt      = column[Instant]("created_at")

  def * =
    (id, orderPaymentId, stripeChargeId, state, currency, amount, deletedAt, createdAt) <> ((ApplePayCharge.apply _).tupled, ApplePayCharge.unapply)
}

object ApplePayCharges
    extends FoxTableQuery[ApplePayCharge, ApplePayCharges](new ApplePayCharges(_))
    with ReturningId[ApplePayCharge, ApplePayCharges] {

  def authFromStripe(ap: ApplePayment,
                     pmt: OrderPayment,
                     stripeCharge: StripeCharge,
                     currency: Currency) =
    ApplePayCharge(orderPaymentId = pmt.id,
                   stripeChargeId = stripeCharge.getId,
                   state = Auth,
                   currency = currency,
                   amount = stripeCharge.getAmount.toInt)

  val returningLens: Lens[ApplePayCharge, Int] = lens[ApplePayCharge].id
}
