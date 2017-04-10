package models.payment.applepay

import java.time.Instant

import com.pellucid.sealerate
import models.cord.OrderPayment
import shapeless.{Lens, lens}
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import slick.lifted._
import utils.ADT
import utils.Money.Currency
import utils.aliases.stripe.StripeCharge
import utils.db._

case class ApplePayCharge(id: Int = 0,
                          orderPaymentId: Int,
                          stripeChargeId: String,
                          state: ApplePayCharge.State = ApplePayCharge.Cart,
                          currency: Currency = Currency.USD,
                          amount: Int = 0,
                          deletedAt: Option[Instant] = None,
                          createdAt: Instant = Instant.now())
    extends FoxModel[ApplePayCharge] {
  // todo FSM state transition
}

object ApplePayCharge {
  sealed trait State
  case object Cart          extends State
  case object Auth          extends State
  case object FailedAuth    extends State
  case object CanceledAuth  extends State
  case object FailedCapture extends State
  case object FullCapture   extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn

}

class ApplePayCharges(tag: Tag) extends FoxTable[ApplePayCharge](tag, "apple_pay_charges") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderPaymentId = column[Int]("order_payment_id")
  def stripeChargeId = column[String]("stripe_charge_id")
  def state          = column[ApplePayCharge.State]("state")
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
  import ApplePayCharge._

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
