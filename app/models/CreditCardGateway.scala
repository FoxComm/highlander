package models

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import payloads.{CreditCardPayload, CreateCustomerPayload}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import utils._
import com.wix.accord.Validator
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}
import org.scalactic.{Or, ErrorMessage}
import com.stripe.model.{Customer => StripeCustomer}

case class CreditCardGateway(id: Int = 0, customerId: Int, gatewayCustomerId: String, lastFour: String,
                             expMonth: Int, expYear: Int)
  extends ModelWithIdParameter
  with Validation[CreditCardGateway]
  with PaymentMethod {

  def authenticate(amount: Int)(implicit ec: ExecutionContext): Future[String Or List[ErrorMessage]] = {
    Future.successful("all good!")
  }

  override def validator = createValidator[CreditCardGateway] { cc =>
    cc.lastFour should matchRegex("[0-9]{4}")
    cc.expYear is between(2015, 2050)
    cc.expMonth is between(1, 12)
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

  def _findById(id: Rep[Int]) = { this.filter(_.id === id) }
}

