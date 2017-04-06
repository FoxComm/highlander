package models.payment.applepay

import java.time.Instant

import akka.actor.FSM
import com.pellucid.sealerate
import models.cord.OrderPayment
import models.payment.applepay.ApplePayCharge.STATUS_SUCCESS
import slick.driver.PostgresDriver.api._
import shapeless.{Lens, lens}
import slick.ast.BaseTypedType
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
                          state: ApplePayCharge.State = ApplePayCharge.CART,
                          currency: Currency = Currency.USD,
                          amount: Int = 0,
                          deletedAt: Option[Instant] = None,
                          createdAt: Instant = Instant.now())
    extends FoxModel[ApplePayCharge] {}

object ApplePayCharge {
  sealed trait State
  object CART           extends State
  object STATUS_SUCCESS extends State
  object STATUS_FAILURE extends State

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

  def authFromStripe(ap: ApplePayment,
                     pmt: OrderPayment,
                     stripeCharge: StripeCharge,
                     currency: Currency) =
    ApplePayCharge(orderPaymentId = pmt.id,
                   chargeId = stripeCharge.getId,
                   gatewayCustomerId = stripeCharge.getCustomer,
                   state = STATUS_SUCCESS,
                   currency = currency,
                   amount = stripeCharge.getAmount.toInt)

  def authorizedOrderPayments(p: Seq[Int]) = ???

  val returningLens: Lens[ApplePayCharge, Int] = lens[ApplePayCharge].id
}
