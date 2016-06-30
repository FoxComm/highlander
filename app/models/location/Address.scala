package models.location

import java.time.Instant

import cats.data.Xor
import cats.data.Xor.{left, right}
import failures.{Failures, NotFoundFailure404}
import models.order.OrderShippingAddress
import models.payment.creditcard.CreditCard
import models.traits.Addressable
import payloads.AddressPayloads.CreateAddressPayload
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.Validation
import utils.db._

case class Address(id: Int = 0,
                   customerId: Int,
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

  def instance: Address = { this }
  def zipLens           = lens[Address].zip
  override def sanitize = super.sanitize(this)
  override def validate = super.validate

  def mustBelongToCustomer(customerId: Int): Failures Xor Address =
    if (this.isNew || this.customerId == customerId) right(this)
    else left(NotFoundFailure404(Address, this.id).single)
}

object Address {
  val zipPattern   = "(?i)^[a-z0-9][a-z0-9\\- ]{0,10}[a-z0-9]$"
  val zipPatternUs = "^\\d{5}(?:\\d{4})?$"

  def fromPayload(p: CreateAddressPayload): Address =
    Address(customerId = 0,
            regionId = p.regionId,
            name = p.name,
            address1 = p.address1,
            address2 = p.address2,
            city = p.city,
            zip = p.zip,
            phoneNumber = p.phoneNumber)

  def fromOrderShippingAddress(osa: OrderShippingAddress): Address =
    Address(customerId = 0,
            regionId = osa.regionId,
            name = osa.name,
            address1 = osa.address1,
            address2 = osa.address2,
            city = osa.city,
            zip = osa.zip,
            phoneNumber = osa.phoneNumber)

  def fromCreditCard(cc: CreditCard): Address =
    Address(customerId = 0,
            regionId = cc.regionId,
            name = cc.addressName,
            address1 = cc.address1,
            address2 = cc.address2,
            city = cc.city,
            zip = cc.zip)
}

class Addresses(tag: Tag) extends FoxTable[Address](tag, "addresses") {
  def id                = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId        = column[Int]("customer_id")
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
     customerId,
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

  def findAllByCustomerId(customerId: Int): QuerySeq = filter(_.customerId === customerId)

  def findAllActiveByCustomerId(customerId: Int): QuerySeq =
    findAllByCustomerId(customerId).filter(_.deletedAt.isEmpty)

  def findAllByCustomerIdWithRegions(customerId: Int): AddressesWithRegionsQuery =
    findAllByCustomerId(customerId).withRegions

  def findAllActiveByCustomerIdWithRegions(customerId: Int): AddressesWithRegionsQuery =
    findAllActiveByCustomerId(customerId).withRegions

  def findShippingDefaultByCustomerId(customerId: Int): QuerySeq =
    filter(_.customerId === customerId).filter(_.isDefaultShipping === true)

  def findByIdAndCustomer(addressId: Int, customerId: Int): QuerySeq =
    findById(addressId).extract.filter(_.customerId === customerId)

  def findActiveByIdAndCustomer(addressId: Int, customerId: Int): QuerySeq =
    findByIdAndCustomer(addressId, customerId).filter(_.deletedAt.isEmpty)

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
