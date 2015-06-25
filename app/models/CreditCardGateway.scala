package models

import com.stripe.model.{Customer ⇒ StripeCustomer}
import com.wix.accord.dsl.{validator ⇒ createValidator, _}
import monocle.macros.GenLens
import org.scalactic.{ErrorMessage, Or}
import payloads.CreditCardPayload
import services.StripeGateway
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils._
import validators._

import scala.concurrent.{ExecutionContext, Future}

case class CreditCardGateway(id: Int = 0, customerId: Int, gatewayCustomerId: String, lastFour: String,
                             expMonth: Int, expYear: Int)
  extends PaymentMethod
  with ModelWithIdParameter
  with Validation[CreditCardGateway] {

  def authorize(amount: Int)(implicit ec: ExecutionContext): Future[String Or List[ErrorMessage]] = {
    new StripeGateway().authorizeAmount(gatewayCustomerId, amount)
  }


  override def validator = createValidator[CreditCardGateway] { cc =>
    cc.lastFour should matchRegex("[0-9]{4}")
    cc.expYear  is expirationYear
    cc.expYear  is withinTwentyYears
    cc.expMonth is monthOfYear
    cc.expMonth is expirationMonth
  }
}

object CreditCardGateway {
  def build(c: StripeCustomer, payload: CreditCardPayload): CreditCardGateway = {
    CreditCardGateway(customerId = 0, gatewayCustomerId = c.getId, lastFour = payload.lastFour,
      expMonth = payload.expMonth, expYear = payload.expYear)
  }
}

class CreditCardGateways(tag: Tag)
  extends GenericTable.TableWithId[CreditCardGateway](tag, "credit_card_gateways")
  with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Int]("customer_id")
  def gatewayCustomerId = column[String]("gateway_customer_id")
  def lastFour = column[String]("last_four")
  def expMonth = column[Int]("exp_month")
  def expYear = column[Int]("exp_year")

  def * = (id, customerId, gatewayCustomerId,
    lastFour, expMonth, expYear) <> ((CreditCardGateway.apply _).tupled, CreditCardGateway.unapply)
}

object CreditCardGateways extends TableQueryWithId[CreditCardGateway, CreditCardGateways](
  idLens = GenLens[CreditCardGateway](_.id)
)(new CreditCardGateways(_)) {

  def findById(id: Int)(implicit db: Database): Future[Option[CreditCardGateway]] = {
    db.run(_findById(id).result.headOption)
  }

  def _findById(id: Rep[Int]) = { filter(_.id === id) }
}

