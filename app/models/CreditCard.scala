package models

import scala.concurrent.{ExecutionContext, Future}

import com.stripe.model.{Customer ⇒ StripeCustomer}
import com.wix.accord.dsl.{validator ⇒ createValidator, _}
import monocle.macros.GenLens
import org.scalactic.Or
import payloads.CreditCardPayload
import services.{Failure, StripeGateway}
import com.wix.accord.dsl.{validator => createValidator}
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

final case class CreditCard(id: Int = 0, customerId: Int, gatewayCustomerId: String, lastFour: String,
                             expMonth: Int, expYear: Int)
  extends PaymentMethod
  with ModelWithIdParameter
  with Validation[CreditCard] {

  def authorize(amount: Int)(implicit ec: ExecutionContext): Future[String Or List[Failure]] = {
    new StripeGateway().authorizeAmount(gatewayCustomerId, amount)
  }


  override def validator = createValidator[CreditCard] { cc =>
    cc.lastFour should matchRegex("[0-9]{4}")
    cc.expYear as "credit card" is notExpired(year = cc.expYear, month = cc.expMonth)
    cc.expYear as "credit card" is withinTwentyYears(year = cc.expYear, month = cc.expMonth)
  }
}

object CreditCard {
  def build(c: StripeCustomer, payload: CreditCardPayload): CreditCard = {
    CreditCard(customerId = 0, gatewayCustomerId = c.getId, lastFour = payload.lastFour,
      expMonth = payload.expMonth, expYear = payload.expYear)
  }
}

class CreditCards(tag: Tag)
  extends GenericTable.TableWithId[CreditCard](tag, "credit_cards")
  with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Int]("customer_id")
  def gatewayCustomerId = column[String]("gateway_customer_id")
  def lastFour = column[String]("last_four")
  def expMonth = column[Int]("exp_month")
  def expYear = column[Int]("exp_year")

  def * = (id, customerId, gatewayCustomerId,
    lastFour, expMonth, expYear) <> ((CreditCard.apply _).tupled, CreditCard.unapply)
}

object CreditCards extends TableQueryWithId[CreditCard, CreditCards](
  idLens = GenLens[CreditCard](_.id)
)(new CreditCards(_)) {

  def findById(id: Int)(implicit db: Database): Future[Option[CreditCard]] = {
    db.run(_findById(id).result.headOption)
  }

  def _findById(id: Rep[Int]) = { filter(_.id === id) }
}

