package models

import scala.concurrent.Future

import monocle.macros.GenLens
import payloads.CreateAddressPayload
import slick.driver.PostgresDriver.api._
import utils.GenericTable.TableWithId
import utils.Slick.implicits._
import utils.{ModelWithIdParameter, NewModel, TableQueryWithId, Validation}

final case class Address(id: Int = 0, customerId: Int, regionId: Int, name: String,
  address1: String, address2: Option[String], city: String, zip: String,
  isDefaultShipping: Boolean = false, phoneNumber: Option[String] = None)
  extends ModelWithIdParameter
  with NewModel
  with Addressable[Address]
  with Validation[Address] {

  def isNew: Boolean = id == 0

  def instance: Address = { this }
  def zipLens = GenLens[Address](_.zip)
}

object Address {
  val zipPattern = "(?i)^[a-z0-9][a-z0-9\\- ]{0,10}[a-z0-9]$"
  val zipPatternUs = "^\\d{5}(?:\\d{4})?$"

  def fromPayload(p: CreateAddressPayload): Address =
    Address(customerId = 0, regionId = p.regionId, name = p.name,
      address1 = p.address1, address2 = p.address2, city = p.city, zip = p.zip, phoneNumber = p.phoneNumber)

  def fromOrderShippingAddress(osa: OrderShippingAddress): Address =
    Address(customerId = 0, regionId = osa.regionId, name = osa.name, address1 = osa.address1, address2 = osa.address2,
      city = osa.city, zip = osa.zip, phoneNumber = osa.phoneNumber)

  def fromCreditCard(cc: CreditCard): Address =
    Address(customerId = 0, regionId = cc.regionId, name = cc.addressName,
      address1 = cc.address1, address2 = cc.address2, city = cc.city, zip = cc.zip)
}

class Addresses(tag: Tag) extends TableWithId[Address](tag, "addresses")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Int]("customer_id")
  def regionId = column[Int]("region_id")
  def name = column[String]("name")
  def address1 = column[String]("address1")
  def address2 = column[Option[String]]("address2")
  def city = column[String]("city")
  def zip = column[String]("zip")
  def isDefaultShipping = column[Boolean]("is_default_shipping")
  def phoneNumber = column[Option[String]]("phone_number")

  def * = (id, customerId, regionId, name, address1, address2,
    city, zip, isDefaultShipping, phoneNumber) <> ((Address.apply _).tupled, Address.unapply)

  def region = foreignKey(Regions.tableName, regionId, Regions)(_.id)
}

object Addresses extends TableQueryWithId[Address, Addresses](
  idLens = GenLens[Address](_.id)
  )(new Addresses(_)) {

  import scope._

  def findAllByCustomer(customer: Customer)(implicit db: Database): Future[Seq[Address]] = {
    findAllByCustomerId(customer.id)
  }

  def findAllByCustomerId(customerId: Int)(implicit db: Database): Future[Seq[Address]] =
    _findAllByCustomerId(customerId).result.run()

  def _findAllByCustomerId(customerId: Int): QuerySeq =
    filter(_.customerId === customerId)

  def _findAllByCustomerIdWithRegions(customerId: Int): Query[(Addresses, Regions), (Address, Region), Seq] = for {
    (addresses, regions) ← _findAllByCustomerId(customerId).withRegions
  } yield (addresses, regions)

  def findShippingDefaultByCustomerId(customerId: Int): QuerySeq =
   filter(_.customerId === customerId).filter(_.isDefaultShipping === true)

  def findById(customerId: Int, addressId: Int): QuerySeq = 
   filter(_.id === addressId).filter(_.customerId === customerId)

  object scope {
    implicit class AddressesQuerySeqConversions(q: QuerySeq) {
      def withRegions: Query[(Addresses, Regions), (Address, Region), Seq] = for {
        addresses ← q
        regions ← Regions if regions.id === addresses.regionId
      } yield (addresses, regions)
    }
  }
}
