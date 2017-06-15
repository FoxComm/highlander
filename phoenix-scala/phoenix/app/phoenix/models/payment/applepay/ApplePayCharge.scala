package phoenix.models.payment.applepay

import java.time.Instant

import phoenix.models.cord.OrderPayment
import phoenix.models.payment.ExternalCharge._
import phoenix.models.payment.ExternalCharge
import phoenix.utils.aliases.stripe.StripeCharge
import shapeless.{lens, Lens}
import slick.jdbc.PostgresProfile.api._
import slick.lifted._
import core.utils.Money.Currency
import core.db._

case class ApplePayCharge(id: Int = 0,
                          orderPaymentId: Int,
                          stripeChargeId: String,
                          state: State = Cart,
                          currency: Currency = Currency.USD,
                          amount: Long = 0,
                          deletedAt: Option[Instant] = None,
                          createdAt: Instant = Instant.now())
    extends ExternalCharge[ApplePayCharge] {

  override def updateModelState(s: State)(implicit ec: EC): DbResultT[Unit] =
    for {
      _ ← * <~ transitionState(s)
      _ ← * <~ ApplePayCharges.filter(_.id === id).map(_.state).update(s)
    } yield ()

  def stateLens = lens[ApplePayCharge].state
}

class ApplePayCharges(tag: Tag) extends FoxTable[ApplePayCharge](tag, "apple_pay_charges") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderPaymentId = column[Int]("order_payment_id")
  def stripeChargeId = column[String]("stripe_charge_id")
  def state          = column[State]("state")
  def currency       = column[Currency]("currency")
  def amount         = column[Long]("amount")
  def deletedAt      = column[Option[Instant]]("deleted_at")
  def createdAt      = column[Instant]("created_at")

  def * =
    (id, orderPaymentId, stripeChargeId, state, currency, amount, deletedAt, createdAt) <> ((ApplePayCharge.apply _).tupled, ApplePayCharge.unapply)
}

object ApplePayCharges
    extends FoxTableQuery[ApplePayCharge, ApplePayCharges](new ApplePayCharges(_))
    with ReturningId[ApplePayCharge, ApplePayCharges] {

  def authFromStripe(ap: ApplePayment, pmt: OrderPayment, stripeCharge: StripeCharge, currency: Currency) =
    ApplePayCharge(orderPaymentId = pmt.id,
                   stripeChargeId = stripeCharge.getId,
                   state = Auth,
                   currency = currency,
                   amount = stripeCharge.getAmount)

  val returningLens: Lens[ApplePayCharge, Int] = lens[ApplePayCharge].id
}
