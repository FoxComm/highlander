package models.payment.creditcard

import java.time.Instant

import cats.data.{ValidatedNel, Xor}
import cats.implicits._
import failures.CreditCardFailures.CannotUseInactiveCreditCard
import failures._
import models.customer.{Customer, Customers}
import models.location._
import models.payment.PaymentMethod
import models.traits.Addressable
import payloads.PaymentPayloads._
import shapeless._
import slick.driver.PostgresDriver.api._
import utils._
import utils.aliases._
import utils.aliases.stripe._
import utils.db._

case class CreditCard(id: Int = 0,
                      parentId: Option[Int] = None,
                      customerId: Int,
                      gatewayCustomerId: String,
                      gatewayCardId: String,
                      holderName: String,
                      lastFour: String,
                      expMonth: Int,
                      expYear: Int,
                      isDefault: Boolean = false,
                      inWallet: Boolean = true,
                      deletedAt: Option[Instant] = None,
                      regionId: Int,
                      addressName: String,
                      address1: String,
                      address2: Option[String] = None,
                      city: String,
                      zip: String,
                      brand: String)
    extends PaymentMethod
    with FoxModel[CreditCard]
    with Addressable[CreditCard]
    with Validation[CreditCard] {

  import Validation._

  // must be implemented for Addressable
  def name: String                      = addressName
  def phoneNumber: Option[String]       = None
  def zipLens: Lens[CreditCard, String] = lens[CreditCard].zip

  override def validate: ValidatedNel[Failure, CreditCard] = {
    (matches(lastFour, "[0-9]{4}", "lastFour") |@| notExpired(
            expYear,
            expMonth,
            "credit card is expired") |@| withinNumberOfYears(
            expYear,
            expMonth,
            20,
            "credit card expiration is too far in the future") |@| super.validate).map {
      case _ ⇒ this
    }
  }

  def mustBeInWallet: Failures Xor CreditCard =
    if (inWallet) Xor.right(this) else Xor.left(CannotUseInactiveCreditCard(this).single)

  def mustBelongToCustomer(ownerId: Int): Failures Xor CreditCard =
    if (customerId == ownerId) Xor.right(this)
    else Xor.left(NotFoundFailure400(CreditCard, id).single)

  def copyFromAddress(a: Address): CreditCard =
    this.copy(regionId = a.regionId,
              addressName = a.name,
              address1 = a.address1,
              address2 = a.address2,
              city = a.city,
              zip = a.zip)
}

object CreditCard {
  def buildFromToken(customer: Customer,
                     stripeCustomer: StripeCustomer,
                     stripeCard: StripeCard,
                     payload: CreateCreditCardFromTokenPayload,
                     address: Address): CreditCard =
    CreditCard(customerId = customer.id,
               gatewayCustomerId = stripeCustomer.getId,
               gatewayCardId = stripeCard.getId,
               brand = payload.brand,
               lastFour = payload.lastFour,
               expMonth = payload.expMonth,
               expYear = payload.expYear,
               holderName = payload.holderName,
               addressName = address.name,
               regionId = address.regionId,
               address1 = address.address1,
               address2 = address.address2,
               zip = address.zip,
               city = address.city)

  @deprecated(message = "Use `buildFromToken` instead", "Until we are PCI compliant")
  def buildFromSource(customer: Customer,
                      stripeCustomer: StripeCustomer,
                      stripeCard: StripeCard,
                      payload: CreateCreditCardFromSourcePayload,
                      address: Address): CreditCard = {
    CreditCard(customerId = customer.id,
               gatewayCustomerId = stripeCustomer.getId,
               gatewayCardId = stripeCard.getId,
               holderName = payload.holderName,
               lastFour = payload.lastFour,
               expMonth = payload.expMonth,
               expYear = payload.expYear,
               isDefault = payload.isDefault,
               regionId = address.regionId,
               addressName = address.name,
               address1 = address.address1,
               address2 = address.address2,
               city = address.city,
               zip = address.zip,
               brand = stripeCard.getBrand)
  }
}

class CreditCards(tag: Tag) extends FoxTable[CreditCard](tag, "credit_cards") {

  def id                = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def parentId          = column[Option[Int]]("parent_id")
  def customerId        = column[Int]("customer_id")
  def gatewayCustomerId = column[String]("gateway_customer_id")
  def gatewayCardId     = column[String]("gateway_card_id")
  def holderName        = column[String]("holder_name")
  def lastFour          = column[String]("last_four")
  def expMonth          = column[Int]("exp_month")
  def expYear           = column[Int]("exp_year")
  def brand             = column[String]("brand")
  def isDefault         = column[Boolean]("is_default")
  def inWallet          = column[Boolean]("in_wallet")
  def deletedAt         = column[Option[Instant]]("deleted_at")
  def regionId          = column[Int]("region_id")
  def addressName       = column[String]("address_name")
  def address1          = column[String]("address1")
  def address2          = column[Option[String]]("address2")
  def city              = column[String]("city")
  def zip               = column[String]("zip")

  def * =
    (id,
     parentId,
     customerId,
     gatewayCustomerId,
     gatewayCardId,
     holderName,
     lastFour,
     expMonth,
     expYear,
     isDefault,
     inWallet,
     deletedAt,
     regionId,
     addressName,
     address1,
     address2,
     city,
     zip,
     brand) <> ((CreditCard.apply _).tupled, CreditCard.unapply)

  def customer = foreignKey(Customers.tableName, customerId, Customers)(_.id)
  def region   = foreignKey(Regions.tableName, regionId, Regions)(_.id)
}

object CreditCards
    extends FoxTableQuery[CreditCard, CreditCards](new CreditCards(_))
    with ReturningId[CreditCard, CreditCards] {

  val returningLens: Lens[CreditCard, Int] = lens[CreditCard].id

  def findInWalletByCustomerId(customerId: Int): QuerySeq =
    filter(_.customerId === customerId).filter(_.inWallet === true)

  def findDefaultByCustomerId(customerId: Int): QuerySeq =
    findInWalletByCustomerId(customerId).filter(_.isDefault === true)

  def findByIdAndCustomerId(id: Int, customerId: Int): QuerySeq =
    filter(_.customerId === customerId).filter(_.id === id)

  def mustFindByIdAndCustomer(id: Int, customerId: Int)(implicit ec: EC): DbResultT[CreditCard] = {
    filter(cc ⇒ cc.id === id && cc.customerId === customerId).one.dbresult.flatMap {
      case Some(cc) ⇒ DbResultT.good(cc)
      case None     ⇒ DbResultT.failure(NotFoundFailure404(CreditCard, id))
    }
  }
}
