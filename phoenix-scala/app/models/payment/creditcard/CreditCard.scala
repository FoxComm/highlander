package models.payment.creditcard

import java.time.Instant

import cats.data.{ValidatedNel, Xor}
import cats.implicits._
import failures.CreditCardFailures.CannotUseInactiveCreditCard
import failures._
import models.account._
import models.location._
import models.payment.PaymentMethod
import models.traits.Addressable
import payloads.PaymentPayloads.{CreateCreditCardFromSourcePayload, CreateCreditCardFromTokenPayload}
import shapeless._
import slick.driver.PostgresDriver.api._
import utils._
import utils.aliases._
import utils.aliases.stripe._
import utils.db._

case class CreditCard(id: Int = 0,
                      parentId: Option[Int] = None,
                      accountId: Int,
                      gatewayCustomerId: String,
                      gatewayCardId: String,
                      holderName: String,
                      lastFour: String,
                      expMonth: Int,
                      expYear: Int,
                      isDefault: Boolean = false,
                      address1Check: Option[String] = None,
                      zipCheck: Option[String] = None,
                      inWallet: Boolean = true,
                      deletedAt: Option[Instant] = None,
                      regionId: Int,
                      addressName: String,
                      address1: String,
                      address2: Option[String] = None,
                      city: String,
                      zip: String,
                      brand: String,
                      phoneNumber: Option[String] = None)
    extends PaymentMethod
    with FoxModel[CreditCard]
    with Addressable[CreditCard]
    with Validation[CreditCard] {

  import Validation._

  // must be implemented for Addressable
  def name: String                      = addressName
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

  def mustBelongToAccount(ownerId: Int): Failures Xor CreditCard =
    if (accountId == ownerId) Xor.right(this)
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
  def buildFromToken(accountId: Int,
                     customerToken: String,
                     cardToken: String,
                     payload: CreateCreditCardFromTokenPayload,
                     address: Address): CreditCard =
    CreditCard(accountId = accountId,
               gatewayCustomerId = customerToken,
               gatewayCardId = cardToken,
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
               city = address.city,
               phoneNumber = address.phoneNumber)

  @deprecated(message = "Use `buildFromToken` instead", "Until we are PCI compliant")
  def buildFromSource(accountId: Int,
                      sCust: StripeCustomer,
                      card: StripeCard,
                      p: CreateCreditCardFromSourcePayload,
                      a: Address): CreditCard = {
    CreditCard(accountId = accountId,
               gatewayCustomerId = sCust.getId,
               gatewayCardId = card.getId,
               holderName = p.holderName,
               lastFour = p.lastFour,
               expMonth = p.expMonth,
               expYear = p.expYear,
               isDefault = p.isDefault,
               address1Check = card.getAddressLine1Check.some,
               zipCheck = card.getAddressZipCheck.some,
               regionId = a.regionId,
               addressName = a.name,
               address1 = a.address1,
               address2 = a.address2,
               city = a.city,
               zip = a.zip,
               brand = card.getBrand,
               phoneNumber = a.phoneNumber)
  }
}

class CreditCards(tag: Tag) extends FoxTable[CreditCard](tag, "credit_cards") {

  def id                = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def parentId          = column[Option[Int]]("parent_id")
  def accountId         = column[Int]("account_id")
  def gatewayCustomerId = column[String]("gateway_customer_id")
  def gatewayCardId     = column[String]("gateway_card_id")
  def holderName        = column[String]("holder_name")
  def lastFour          = column[String]("last_four")
  def expMonth          = column[Int]("exp_month")
  def expYear           = column[Int]("exp_year")
  def brand             = column[String]("brand")
  def isDefault         = column[Boolean]("is_default")
  def address1Check     = column[Option[String]]("address1_check")
  def zipCheck          = column[Option[String]]("zip_check")
  def inWallet          = column[Boolean]("in_wallet")
  def deletedAt         = column[Option[Instant]]("deleted_at")

  def regionId    = column[Int]("region_id")
  def addressName = column[String]("address_name")
  def address1    = column[String]("address1")
  def address2    = column[Option[String]]("address2")
  def city        = column[String]("city")
  def zip         = column[String]("zip")
  def phoneNumber = column[Option[String]]("phone_number")

  def * =
    (id,
     parentId,
     accountId,
     gatewayCustomerId,
     gatewayCardId,
     holderName,
     lastFour,
     expMonth,
     expYear,
     isDefault,
     address1Check,
     zipCheck,
     inWallet,
     deletedAt,
     regionId,
     addressName,
     address1,
     address2,
     city,
     zip,
     brand,
     phoneNumber) <> ((CreditCard.apply _).tupled, CreditCard.unapply)

  def account = foreignKey(Accounts.tableName, accountId, Accounts)(_.id)
  def region  = foreignKey(Regions.tableName, regionId, Regions)(_.id)
}

object CreditCards
    extends FoxTableQuery[CreditCard, CreditCards](new CreditCards(_))
    with ReturningId[CreditCard, CreditCards] {

  val returningLens: Lens[CreditCard, Int] = lens[CreditCard].id

  def findInWalletByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId).filter(_.inWallet === true)

  def findDefaultByAccountId(accountId: Int): QuerySeq =
    findInWalletByAccountId(accountId).filter(_.isDefault === true)

  def findByIdAndAccountId(id: Int, accountId: Int): QuerySeq =
    filter(_.accountId === accountId).filter(_.id === id)

  def mustFindByIdAndAccountId(id: Int, accountId: Int)(implicit ec: EC): DbResultT[CreditCard] = {
    filter(cc ⇒ cc.id === id && cc.accountId === accountId).one.dbresult.flatMap {
      case Some(cc) ⇒ DbResultT.good(cc)
      case None     ⇒ DbResultT.failure(NotFoundFailure404(CreditCard, id))
    }
  }
}
