package models

import scala.concurrent.{ExecutionContext, Future}

import com.pellucid.sealerate
import com.stripe.model.{Customer ⇒ StripeCustomer}
import com.wix.accord.dsl.{validator ⇒ createValidator, _}
import monocle.Lens
import monocle.macros.GenLens
import org.scalactic.Or
import payloads.CreateCreditCard
import services.{Failures, StripeGateway}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils._
import validators._

final case class CreditCardCharge(id: Int = 0, creditCardId: Int, orderPaymentId: Int,
  chargeId: String, status: CreditCardCharge.Status = CreditCardCharge.Auth)
  extends ModelWithIdParameter
  with FSM[CreditCardCharge.Status, CreditCardCharge] {

  import CreditCardCharge._

  val state: Status = status
  val stateLens: Lens[CreditCardCharge, Status] = GenLens[CreditCardCharge](_.status)

  val fsm: Map[Status, Set[Status]] = Map(
    Auth →
      Set(FullCapture, FailedCapture, CanceledAuth, ExpiredAuth),
    ExpiredAuth →
      Set(Auth)
  )
}

object CreditCardCharge {
  sealed trait Status
  case object Auth extends Status
  case object ExpiredAuth extends Status
  case object CanceledAuth extends Status
  case object FailedCapture extends Status
  case object FullCapture extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  implicit val statusColumnType = Status.slickColumn
}

class CreditCardCharges(tag: Tag)
  extends GenericTable.TableWithId[CreditCardCharge](tag, "credit_card_charges")
  with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def creditCardId = column[Int]("credit_card_id")
  def orderPaymentId = column[Int]("order_payment_id")
  def chargeId = column[String]("charge_id")
  def status = column[CreditCardCharge.Status]("status")

  def * = (id, creditCardId, orderPaymentId, chargeId, status) <>
    ((CreditCardCharge.apply _).tupled, CreditCardCharge.unapply)

  def creditCard        = foreignKey(CreditCards.tableName, creditCardId, CreditCards)(_.id)
  def orderPayment      = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id)
}

object CreditCardCharges extends TableQueryWithId[CreditCardCharge, CreditCardCharges](
  idLens = GenLens[CreditCardCharge](_.id)
)(new CreditCardCharges(_)) {
}


