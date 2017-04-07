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
                          gatewayCustomerId: String,
                          orderPaymentId: Int,
                          chargeId: String,
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

  // utilize this objects for response
//  case object STATUS_SUCCESS extends State
//  case object STATUS_FAILURE extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn

}

class ApplePayCharges(tag: Tag) extends FoxTable[ApplePayCharge](tag, "apple_pay_charges") {
  def id                = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def gatewayCustomerId = column[String]("gateway_customer_id")
  def orderPaymentId    = column[Int]("order_payment_id")
  def chargeId          = column[String]("charge_id")
  def state             = column[ApplePayCharge.State]("state")
  def currency          = column[Currency]("currency")
  def amount            = column[Int]("amount")
  def deletedAt         = column[Option[Instant]]("deleted_at")
  def createdAt         = column[Instant]("created_at")

  def * =
    (id,
     gatewayCustomerId,
     orderPaymentId,
     chargeId,
     state,
     currency,
     amount,
     deletedAt,
     createdAt) <> ((ApplePayCharge.apply _).tupled, ApplePayCharge.unapply)
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
                   chargeId = stripeCharge.getId,
                   gatewayCustomerId = stripeCharge.getCustomer,
                   state = Auth,
                   currency = currency,
                   amount = stripeCharge.getAmount.toInt)

  val returningLens: Lens[ApplePayCharge, Int] = lens[ApplePayCharge].id
}
