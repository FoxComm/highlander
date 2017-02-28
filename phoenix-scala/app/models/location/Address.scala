package models.location

import java.time.Instant

import cats.data.Xor
import cats.data.Xor.{left, right}
import failures.{Failures, NotFoundFailure404}
import models.cord.OrderShippingAddress
import models.payment.creditcard.CreditCard
import models.traits.Addressable
import payloads.AddressPayloads.CreateAddressPayload
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.Validation
import utils.db._

case class Address(id: Int = 0,
                   accountId: Int,
                   regionId: Int,
                   name: String,
                   address1: String,
                   address2: Option[String],
                   city: String,
                   zip: String,
                   isDefaultShipping: Boolean = false,
                   phoneNumber: Option[String] = None,
                   deletedAt: Option[Instant] = None)
    extends FoxModel[Address]
    with Addressable[Address]
    with Validation[Address] {

  def zipLens = lens[Address].zip

  override def sanitize = super.sanitize(this)
  override def validate = super.validate

  def mustBelongToAccount(accountId: Int): Failures Xor Address =
    if (this.isNew || this.accountId == accountId) right(this)
    else left(NotFoundFailure404(Address, this.id).single)
}

object Address {
  val zipPattern   = "(?i)^[a-z0-9][a-z0-9\\- ]{0,10}[a-z0-9]$"
  val zipPatternUs = "^\\d{5}(?:\\d{4})?$"

  def fromPayload(p: CreateAddressPayload, accountId: Int): Address =
    Address(accountId = accountId,
            regionId = p.regionId,
            name = p.name,
            address1 = p.address1,
            address2 = p.address2,
            city = p.city,
            zip = p.zip,
            phoneNumber = p.phoneNumber)

  def fromOrderShippingAddress(osa: OrderShippingAddress): Address =
    Address(accountId = 0,
            regionId = osa.regionId,
            name = osa.name,
            address1 = osa.address1,
            address2 = osa.address2,
            city = osa.city,
            zip = osa.zip,
            phoneNumber = osa.phoneNumber)

  def fromCreditCard(cc: CreditCard): Address =
    Address(accountId = 0,
            regionId = cc.address.regionId,
            name = cc.address.name,
            address1 = cc.address.address1,
            address2 = cc.address.address2,
            city = cc.address.city,
            zip = cc.address.zip,
            phoneNumber = cc.address.phoneNumber)
}

class Addresses(tag: Tag) extends FoxTable[Address](tag, "addresses") {
  def id                = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId         = column[Int]("account_id")
  def regionId          = column[Int]("region_id")
  def name              = column[String]("name")
  def address1          = column[String]("address1")
  def address2          = column[Option[String]]("address2")
  def city              = column[String]("city")
  def zip               = column[String]("zip")
  def isDefaultShipping = column[Boolean]("is_default_shipping")
  def phoneNumber       = column[Option[String]]("phone_number")
  def deletedAt         = column[Option[Instant]]("deleted_at")

  def * =
    (id,
     accountId,
     regionId,
     name,
     address1,
     address2,
     city,
     zip,
     isDefaultShipping,
     phoneNumber,
     deletedAt) <> ((Address.apply _).tupled, Address.unapply)

  def region = foreignKey(Regions.tableName, regionId, Regions)(_.id)
}

object Addresses
    extends FoxTableQuery[Address, Addresses](new Addresses(_))
    with ReturningId[Address, Addresses] {

  val returningLens: Lens[Address, Int] = lens[Address].id

  import scope._

  type AddressesWithRegionsQuery = Query[(Addresses, Regions), (Address, Region), Seq]

  def findAllByAccountId(accountId: Int): QuerySeq = filter(_.accountId === accountId)

  def findAllActiveByAccountId(accountId: Int): QuerySeq =
    findAllByAccountId(accountId).filter(_.deletedAt.isEmpty)

  def findAllByAccountIdWithRegions(accountId: Int): AddressesWithRegionsQuery =
    findAllByAccountId(accountId).withRegions

  def findAllActiveByAccountIdWithRegions(accountId: Int): AddressesWithRegionsQuery =
    findAllActiveByAccountId(accountId).withRegions

  def findShippingDefaultByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId).filter(_.isDefaultShipping === true)

  def findByIdAndAccount(addressId: Int, accountId: Int): QuerySeq =
    findById(addressId).filter(_.accountId === accountId)

  def findActiveByIdAndAccount(addressId: Int, accountId: Int): QuerySeq =
    findByIdAndAccount(addressId, accountId).filter(_.deletedAt.isEmpty)

  object scope {
    implicit class AddressesQuerySeqConversions(q: QuerySeq) {
      def withRegions: AddressesWithRegionsQuery =
        for {
          addresses ← q
          regions   ← Regions if regions.id === addresses.regionId
        } yield (addresses, regions)
    }
  }
}
