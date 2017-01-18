package models.payment.creditcard

import java.time.Instant

import cats.data.Xor
import com.pellucid.sealerate
import failures.Failures
import models.cord.{OrderPayment, OrderPayments}
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.Money.Currency
import utils._
import utils.aliases.stripe._
import utils.db._

case class CreditCardCharge(id: Int = 0,
                            creditCardId: Int,
                            orderPaymentId: Int,
                            chargeId: String,
                            state: CreditCardCharge.State = CreditCardCharge.Cart,
                            currency: Currency = Currency.USD,
                            amount: Int,
                            createdAt: Instant = Instant.now)
    extends FoxModel[CreditCardCharge]
    with FSM[CreditCardCharge.State, CreditCardCharge] {

  import CreditCardCharge._

  def stateLens = lens[CreditCardCharge].state
  override def updateTo(newModel: CreditCardCharge): Failures Xor CreditCardCharge =
    super.transitionModel(newModel)

  val fsm: Map[State, Set[State]] = Map(
    Cart →
      Set(Auth),
    Auth →
      Set(FullCapture, FailedCapture, CanceledAuth, ExpiredAuth),
    ExpiredAuth →
      Set(Auth)
  )
}

object CreditCardCharge {
  sealed trait State
  case object Cart          extends State
  case object Auth          extends State
  case object FailedAuth    extends State
  case object ExpiredAuth   extends State
  case object CanceledAuth  extends State
  case object FailedCapture extends State
  case object FullCapture   extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn

  def authFromStripe(card: CreditCard,
                     pmt: OrderPayment,
                     stripe: StripeCharge,
                     currency: Currency): CreditCardCharge =
    CreditCardCharge(creditCardId = card.id,
                     orderPaymentId = pmt.id,
                     chargeId = stripe.getId,
                     state = Auth,
                     currency = currency,
                     amount = stripe.getAmount)
}

class CreditCardCharges(tag: Tag) extends FoxTable[CreditCardCharge](tag, "credit_card_charges") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def creditCardId   = column[Int]("credit_card_id")
  def orderPaymentId = column[Int]("order_payment_id")
  def chargeId       = column[String]("charge_id")
  def state          = column[CreditCardCharge.State]("state")
  def currency       = column[Currency]("currency")
  def amount         = column[Int]("amount")
  def createdAt      = column[Instant]("created_at")

  def * =
    (id, creditCardId, orderPaymentId, chargeId, state, currency, amount, createdAt) <>
      ((CreditCardCharge.apply _).tupled, CreditCardCharge.unapply)

  def card         = foreignKey(CreditCards.tableName, creditCardId, CreditCards)(_.id)
  def orderPayment = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id)
}

object CreditCardCharges
    extends FoxTableQuery[CreditCardCharge, CreditCardCharges](new CreditCardCharges(_))
    with ReturningId[CreditCardCharge, CreditCardCharges] {

  val returningLens: Lens[CreditCardCharge, Int] = lens[CreditCardCharge].id
}
