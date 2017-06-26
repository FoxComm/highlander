package phoenix.models.payment.creditcard

import java.time.Instant

import core.db._
import core.utils.Money.Currency
import phoenix.models.cord.{OrderPayment, OrderPayments}
import phoenix.models.payment.ExternalCharge
import phoenix.models.payment.ExternalCharge._
import phoenix.utils.aliases.stripe._
import shapeless._
import slick.jdbc.PostgresProfile.api._

case class CreditCardCharge(id: Int = 0,
                            creditCardId: Int,
                            orderPaymentId: Int,
                            stripeChargeId: String,
                            state: State = Cart,
                            currency: Currency = Currency.USD,
                            amount: Long,
                            createdAt: Instant = Instant.now)
    extends ExternalCharge[CreditCardCharge] {

  override def updateModelState(s: State)(implicit ec: EC): DbResultT[Unit] =
    for {
      _ ← * <~ transitionState(s)
      _ ← * <~ CreditCardCharges.filter(_.id === id).map(_.state).update(s)
    } yield ()

  def stateLens = lens[CreditCardCharge].state

}

object CreditCardCharge {
  def authFromStripe(card: CreditCard,
                     pmt: OrderPayment,
                     stripe: StripeCharge,
                     currency: Currency): CreditCardCharge =
    CreditCardCharge(creditCardId = card.id,
                     orderPaymentId = pmt.id,
                     stripeChargeId = stripe.getId,
                     state = Auth,
                     currency = currency,
                     amount = stripe.getAmount)
}

class CreditCardCharges(tag: Tag) extends FoxTable[CreditCardCharge](tag, "credit_card_charges") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def creditCardId   = column[Int]("credit_card_id")
  def orderPaymentId = column[Int]("order_payment_id")
  def stripeChargeId = column[String]("stripe_charge_id")
  def state          = column[State]("state")
  def currency       = column[Currency]("currency")
  def amount         = column[Long]("amount")
  def createdAt      = column[Instant]("created_at")

  def * =
    (id, creditCardId, orderPaymentId, stripeChargeId, state, currency, amount, createdAt) <>
      ((CreditCardCharge.apply _).tupled, CreditCardCharge.unapply)

  def card         = foreignKey(CreditCards.tableName, creditCardId, CreditCards)(_.id)
  def orderPayment = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id)
}

object CreditCardCharges
    extends FoxTableQuery[CreditCardCharge, CreditCardCharges](new CreditCardCharges(_))
    with ReturningId[CreditCardCharge, CreditCardCharges] {

  val returningLens: Lens[CreditCardCharge, Int] = lens[CreditCardCharge].id
}
