package models

import cats.data.ValidatedNel
import cats.implicits._
import services.Failure
import utils.Litterbox._
import utils.Checks

import scala.concurrent.ExecutionContext

import com.github.tototoshi.slick.PostgresJodaSupport._
import com.stripe.model.{Card ⇒ StripeCard, Customer ⇒ StripeCustomer}
import com.wix.accord.dsl.{validator ⇒ createValidator, _}
import monocle.Lens
import monocle.macros.GenLens
import org.joda.time.DateTime
import payloads.CreateCreditCard
import services.{Result, StripeGateway}
import slick.driver.PostgresDriver.api._
import utils._

final case class CreditCard(id: Int = 0, parentId: Option[Int] = None, customerId: Int, gatewayCustomerId: String,
  gatewayCardId: String, holderName: String, lastFour: String, expMonth: Int, expYear: Int,
  isDefault: Boolean = false, inWallet: Boolean = true, deletedAt: Option[DateTime] = None,
  regionId: Int, addressName: String, street1: String, street2: Option[String] = None, city: String, zip: String)
  extends PaymentMethod
  with ModelWithIdParameter
  with Addressable[CreditCard] {

  def instance: CreditCard = this

  // must be implemented for Addressable
  def name: String = addressName
  def phoneNumber: Option[String] = None
  def zipLens: Lens[CreditCard, String] = GenLens[CreditCard](_.zip)

  def authorize(amount: Int)(implicit ec: ExecutionContext): Result[String] = {
    new StripeGateway().authorizeAmount(gatewayCustomerId, amount)
  }

  override def validateNew: ValidatedNel[Failure, CreditCard] = {
    def withinTwentyYears: Boolean = {
      val today = DateTime.now()
      // At the end of the month
      val expDate = new DateTime(expYear, expMonth, 1, 0, 0).plusMonths(1).minusSeconds(1)
      expDate.isBefore(today.plusYears(20))
    }

    ( Checks.matches(lastFour, "[0-9]{4}", "lastFour")
      |@| Checks.notExpired(expYear, expMonth, "credit card is expired")
      |@| Checks.withinNumberOfYears(expYear, expMonth, 20, "credit card expiration is too far in the future")
      |@| super.validateNew
      ).map { case _ ⇒ this }
  }
}

object CreditCard {
  def build(cust: StripeCustomer, card: StripeCard, p: CreateCreditCard, a: Address): CreditCard = {
    CreditCard(customerId = 0, gatewayCustomerId = cust.getId, gatewayCardId = card.getId, holderName =
      p.holderName, lastFour = p.lastFour, expMonth = p.expMonth, expYear = p.expYear, isDefault = p.isDefault,
      regionId = a.regionId, addressName = a.name, street1 = a.street1, street2 = a.street2,
      city = a.city, zip = a.zip)
  }
}

class CreditCards(tag: Tag)
  extends GenericTable.TableWithId[CreditCard](tag, "credit_cards") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def parentId = column[Option[Int]]("parent_id")
  def customerId = column[Int]("customer_id")
  def gatewayCustomerId = column[String]("gateway_customer_id")
  def gatewayCardId = column[String]("gateway_card_id")
  def holderName = column[String]("holder_name")
  def lastFour = column[String]("last_four")
  def expMonth = column[Int]("exp_month")
  def expYear = column[Int]("exp_year")
  def isDefault = column[Boolean]("is_default")
  def inWallet = column[Boolean]("in_wallet")
  def deletedAt = column[Option[DateTime]]("deleted_at")

  def regionId = column[Int]("region_id")
  def addressName = column[String]("address_name")
  def street1 = column[String]("street1")
  def street2 = column[Option[String]]("street2")
  def city = column[String]("city")
  def zip = column[String]("zip")

  def * = (id, parentId, customerId, gatewayCustomerId, gatewayCardId, holderName,
    lastFour, expMonth, expYear, isDefault, inWallet, deletedAt,
    //blah
    regionId, addressName, street1, street2, city, zip) <> ((CreditCard.apply _).tupled, CreditCard.unapply)

  def customer        = foreignKey(Customers.tableName, customerId, Customers)(_.id)
}

object CreditCards extends TableQueryWithId[CreditCard, CreditCards](
  idLens = GenLens[CreditCard](_.id)
)(new CreditCards(_)) {

  def findInWalletByCustomerId(customerId: Int)(implicit db: Database): Query[CreditCards, CreditCard, Seq] =
    filter(_.customerId === customerId).filter(_.inWallet === true)

  def findDefaultByCustomerId(customerId: Int)(implicit db: Database): Query[CreditCards, CreditCard, Seq] =
    findInWalletByCustomerId(customerId).filter(_.isDefault === true)
}

