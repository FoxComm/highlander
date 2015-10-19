package models

import com.pellucid.sealerate
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils._

final case class CreditCardCharge(id: Int = 0, creditCardId: Int, orderPaymentId: Int,
  chargeId: String, status: CreditCardCharge.Status = CreditCardCharge.Cart)
  extends ModelWithIdParameter
  with FSM[CreditCardCharge.Status, CreditCardCharge] {

  import CreditCardCharge._

  def stateLens = GenLens[CreditCardCharge](_.status)

  val fsm: Map[Status, Set[Status]] = Map(
    Cart →
      Set(Auth),
    Auth →
      Set(FullCapture, FailedCapture, CanceledAuth, ExpiredAuth),
    ExpiredAuth →
      Set(Auth)
  )
}

object CreditCardCharge {
  sealed trait Status
  case object Cart extends Status
  case object Auth extends Status
  case object ExpiredAuth extends Status
  case object CanceledAuth extends Status
  case object FailedCapture extends Status
  case object FullCapture extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  implicit val statusColumnType: JdbcType[Status] with BaseTypedType[Status] = Status.slickColumn
}

class CreditCardCharges(tag: Tag)
  extends GenericTable.TableWithId[CreditCardCharge](tag, "credit_card_charges")
   {

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


