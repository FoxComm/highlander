package models

import scala.concurrent.{ExecutionContext, Future}

import com.stripe.model.{Customer ⇒ StripeCustomer}
import com.wix.accord.dsl.{validator ⇒ createValidator, _}
import monocle.macros.GenLens
import org.scalactic.Or
import payloads.CreateCreditCard
import services.{Failures, StripeGateway}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils._
import validators._

final case class CreditCardCharge(id: Int = 0, creditCardId: Int, orderPaymentId: Int, chargeId: String)
  extends ModelWithIdParameter

class CreditCardCharges(tag: Tag)
  extends GenericTable.TableWithId[CreditCardCharge](tag, "credit_card_charges")
  with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def creditCardId = column[Int]("credit_card_id")
  def orderPaymentId = column[Int]("order_payment_id")
  def chargeId = column[String]("charge_id")

  def * = (id, creditCardId, orderPaymentId, chargeId) <> ((CreditCardCharge.apply _).tupled, CreditCardCharge.unapply)

  def creditCard        = foreignKey(CreditCards.tableName, creditCardId, CreditCards)(_.id)
  def orderPayment      = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id)
}

object CreditCardCharges extends TableQueryWithId[CreditCardCharge, CreditCardCharges](
  idLens = GenLens[CreditCardCharge](_.id)
)(new CreditCardCharges(_)) {
}


