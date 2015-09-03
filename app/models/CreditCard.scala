package models

import cats.data.ValidatedNel
import cats.implicits._
import services.Failure
import utils.Litterbox._
import utils.Checks

import scala.concurrent.{ExecutionContext, Future}

import com.stripe.model.{Customer ⇒ StripeCustomer, Card ⇒ StripeCard}
import monocle.macros.GenLens
import org.joda.time.DateTime

import payloads.CreateCreditCard
import services.StripeGateway
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils._
import services.Result

final case class CreditCard(id: Int = 0, parentId: Option[Int] = None, customerId: Int, billingAddressId: Int = 0,
  gatewayCustomerId: String, gatewayCardId: String, holderName: String, lastFour: String, expMonth: Int, expYear: Int,
  isDefault: Boolean = false, inWallet: Boolean = true, deletedAt: Option[DateTime] = None)
  extends PaymentMethod
  with ModelWithIdParameter {

  def authorize(amount: Int)(implicit ec: ExecutionContext): Result[String] = {
    new StripeGateway().authorizeAmount(gatewayCustomerId, amount)
  }

  def validateNew: ValidatedNel[Failure, CreditCard] = {
    def withinTwentyYears: Boolean = {
      val today = DateTime.now()
      // At the end of the month
      val expDate = new DateTime(expYear, expMonth, 1, 0, 0).plusMonths(1).minusSeconds(1)
      expDate.isBefore(today.plusYears(20))
    }

    ( Checks.matches(lastFour, "[0-9]{4}", "lastFour")
      |@| Checks.notExpired(expYear, expMonth, "credit card is expired")
      |@| Checks.withinNumberOfYears(expYear, expMonth, 20, "credit card expiration is too far in the future")
      ).map { case _ ⇒ this }
  }
}

object CreditCard {
  def build(cust: StripeCustomer, card: StripeCard, p: CreateCreditCard): CreditCard = {
    CreditCard(customerId = 0, gatewayCustomerId = cust.getId, gatewayCardId = card.getId, holderName =
      p.holderName, lastFour = p.lastFour, expMonth = p.expMonth, expYear = p.expYear, isDefault = p.isDefault)
  }
}

class CreditCards(tag: Tag)
  extends GenericTable.TableWithId[CreditCard](tag, "credit_cards")
  with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def parentId = column[Option[Int]]("parent_id")
  def customerId = column[Int]("customer_id")
  def billingAddressId = column[Int]("billing_address_id")
  def gatewayCustomerId = column[String]("gateway_customer_id")
  def gatewayCardId = column[String]("gateway_card_id")
  def holderName = column[String]("holder_name")
  def lastFour = column[String]("last_four")
  def expMonth = column[Int]("exp_month")
  def expYear = column[Int]("exp_year")
  def isDefault = column[Boolean]("is_default")
  def inWallet = column[Boolean]("in_wallet")
  def deletedAt = column[Option[DateTime]]("deleted_at")

  def * = (id, parentId, customerId, billingAddressId, gatewayCustomerId, gatewayCardId, holderName,
    lastFour, expMonth, expYear, isDefault, inWallet, deletedAt) <> ((CreditCard.apply _).tupled, CreditCard
    .unapply)

  def customer        = foreignKey(Customers.tableName, customerId, Customers)(_.id)
  def billingAddress  = foreignKey(Addresses.tableName, billingAddressId, Addresses)(_.id)
}

object CreditCards extends TableQueryWithId[CreditCard, CreditCards](
  idLens = GenLens[CreditCard](_.id)
)(new CreditCards(_)) {

  def findInWalletByCustomerId(customerId: Int)(implicit db: Database): Query[CreditCards, CreditCard, Seq] =
    filter(_.customerId === customerId).filter(_.inWallet === true)

  def findDefaultByCustomerId(customerId: Int)(implicit db: Database): Query[CreditCards, CreditCard, Seq] =
    findInWalletByCustomerId(customerId).filter(_.isDefault === true)
}

