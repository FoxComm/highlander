package models

import java.time.Instant

import cats.implicits._
import models.Orders._
import utils.CustomDirectives.SortAndPage
import utils.Litterbox._

import scala.concurrent.ExecutionContext

import cats.data.{Xor, ValidatedNel}
import com.stripe.model.{Card ⇒ StripeCard, Customer ⇒ StripeCustomer}
import monocle.Lens
import monocle.macros.GenLens
import payloads.CreateCreditCard
import services.{NotFoundFailure404, Failures, CannotUseInactiveCreditCard, Failure}
import slick.driver.PostgresDriver.api._
import utils.Slick.DbResult
import utils._
import utils.Slick.implicits._

final case class CreditCard(id: Int = 0, parentId: Option[Int] = None, customerId: Int, gatewayCustomerId: String,
  gatewayCardId: String, holderName: String, lastFour: String, expMonth: Int, expYear: Int,
  isDefault: Boolean = false, address1Check: Option[String] = None, zipCheck: Option[String] = None,
  inWallet: Boolean = true, deletedAt: Option[Instant] = None, regionId: Int, addressName: String,
  address1: String, address2: Option[String] = None, city: String, zip: String, brand: String)
  extends PaymentMethod
  with ModelWithIdParameter[CreditCard]
  with Addressable[CreditCard]
  with Validation[CreditCard] {

  import Validation._

  def instance: CreditCard = this

  // must be implemented for Addressable
  def name: String = addressName
  def phoneNumber: Option[String] = None
  def zipLens: Lens[CreditCard, String] = GenLens[CreditCard](_.zip)

  override def validate: ValidatedNel[Failure, CreditCard] = {
    ( matches(lastFour, "[0-9]{4}", "lastFour")
      |@| notExpired(expYear, expMonth, "credit card is expired")
      |@| withinNumberOfYears(expYear, expMonth, 20, "credit card expiration is too far in the future")
      |@| super.validate
      ).map { case _ ⇒ this }
  }

  def mustBeInWallet: Failures Xor CreditCard = if (inWallet) Xor.right(this) else Xor.left(CannotUseInactiveCreditCard(this).single)

  def copyFromAddress(a: Address): CreditCard = this.copy(
    regionId = a.regionId, addressName = a.name, address1 = a.address1, address2 = a.address2, city = a.city, zip = a.zip)
}

object CreditCard {
  def build(customerId: Int, sCust: StripeCustomer, card: StripeCard, p: CreateCreditCard, a: Address): CreditCard = {
    CreditCard(customerId = customerId, gatewayCustomerId = sCust.getId, gatewayCardId = card.getId, holderName =
      p.holderName, lastFour = p.lastFour, expMonth = p.expMonth, expYear = p.expYear, isDefault = p.isDefault,
      address1Check = card.getAddressLine1Check.some, zipCheck = card.getAddressZipCheck.some,
      regionId = a.regionId, addressName = a.name, address1 = a.address1, address2 = a.address2,
      city = a.city, zip = a.zip, brand = card.getBrand)
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
  def brand = column[String]("brand")
  def isDefault = column[Boolean]("is_default")
  def address1Check = column[Option[String]]("address1_check")
  def zipCheck = column[Option[String]]("zip_check")
  def inWallet = column[Boolean]("in_wallet")
  def deletedAt = column[Option[Instant]]("deleted_at")

  def regionId = column[Int]("region_id")
  def addressName = column[String]("address_name")
  def address1 = column[String]("address1")
  def address2 = column[Option[String]]("address2")
  def city = column[String]("city")
  def zip = column[String]("zip")

  def * = (id, parentId, customerId, gatewayCustomerId, gatewayCardId, holderName,
    lastFour, expMonth, expYear, isDefault, address1Check, zipCheck, inWallet, deletedAt,
    regionId, addressName, address1, address2, city, zip, brand) <> ((CreditCard.apply _).tupled, CreditCard.unapply)

  def customer        = foreignKey(Customers.tableName, customerId, Customers)(_.id)
  def region          = foreignKey(Regions.tableName, regionId, Regions)(_.id)
}

object CreditCards extends TableQueryWithId[CreditCard, CreditCards](
  idLens = GenLens[CreditCard](_.id)
)(new CreditCards(_)) {

  def sortedAndPaged(query: QuerySeq)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeqWithMetadata = {

    val sortedQuery = sortAndPage.sort match {
      case Some(s) if s.sortColumn == "expDate" ⇒ query.withMetadata.sortBy { creditCard ⇒
        if (s.asc) (creditCard.expYear.asc, creditCard.expMonth.asc)
        else (creditCard.expYear.desc, creditCard.expMonth.desc)
      }
      case Some(s) ⇒ query.withMetadata.sortBy { creditCard ⇒
        s.sortColumn match {
          case "id"                  ⇒ if(s.asc) creditCard.id.asc                else creditCard.id.desc
          case "parentId"            ⇒ if(s.asc) creditCard.parentId.asc          else creditCard.parentId.desc
          case "gatewayCustomerId"   ⇒ if(s.asc) creditCard.gatewayCustomerId.asc else creditCard.gatewayCustomerId.desc
          case "gatewayCardId"       ⇒ if(s.asc) creditCard.gatewayCardId.asc     else creditCard.gatewayCardId.desc
          case "holderName"          ⇒ if(s.asc) creditCard.holderName.asc        else creditCard.holderName.desc
          case "lastFour"            ⇒ if(s.asc) creditCard.lastFour.asc          else creditCard.lastFour.desc
          case "isDefault"           ⇒ if(s.asc) creditCard.isDefault.asc         else creditCard.isDefault.desc
          case "address1Check"       ⇒ if(s.asc) creditCard.address1Check.asc     else creditCard.address1Check.desc
          case "zipCheck"            ⇒ if(s.asc) creditCard.zipCheck.asc          else creditCard.zipCheck.desc
          case "inWallet"            ⇒ if(s.asc) creditCard.inWallet.asc          else creditCard.inWallet.desc
          case "deletedAt"           ⇒ if(s.asc) creditCard.deletedAt.asc         else creditCard.deletedAt.desc
          case "regionId"            ⇒ if(s.asc) creditCard.regionId.asc          else creditCard.regionId.desc
          case "addressName"         ⇒ if(s.asc) creditCard.addressName.asc       else creditCard.addressName.desc
          case "address1"            ⇒ if(s.asc) creditCard.address1.asc          else creditCard.address1.desc
          case "address2"            ⇒ if(s.asc) creditCard.address2.asc          else creditCard.address2.desc
          case "city"                ⇒ if(s.asc) creditCard.city.asc              else creditCard.city.desc
          case "zip"                 ⇒ if(s.asc) creditCard.zip.asc               else creditCard.zip.desc
          case other                 ⇒ invalidSortColumn(other)
        }
      }
      case None    ⇒ query.withMetadata
    }

    sortedQuery.paged
  }

  def findInWalletByCustomerId(customerId: Int)(implicit db: Database): Query[CreditCards, CreditCard, Seq] =
    filter(_.customerId === customerId).filter(_.inWallet === true)

  def findDefaultByCustomerId(customerId: Int)(implicit db: Database): Query[CreditCards, CreditCard, Seq] =
    findInWalletByCustomerId(customerId).filter(_.isDefault === true)

  def mustFindByIdAndCustomer(id: Int, customerId: Int)
    (implicit ec: ExecutionContext): DbResult[CreditCard] = {
    filter(cc ⇒ cc.id === id && cc.customerId === customerId).one.flatMap {
      case Some(cc) ⇒ DbResult.good(cc)
      case None     ⇒ DbResult.failure(NotFoundFailure404(CreditCard, id))
    }
  }
}

