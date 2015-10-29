package models

import java.time.Instant

import scala.concurrent.ExecutionContext

import monocle.macros.GenLens
import payloads.CreateAddressPayload
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.GenericTable.TableWithId
import utils.Slick.implicits._
import utils.{ModelWithIdParameter, NewModel, TableQueryWithId, Validation}

final case class Address(id: Int = 0, customerId: Int, regionId: Int, name: String,
  address1: String, address2: Option[String], city: String, zip: String,
  isDefaultShipping: Boolean = false, phoneNumber: Option[String] = None, 
  deletedAt: Option[Instant] = None)
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
  def deletedAt = column[Option[Instant]]("deleted_at")

  def * = (id, customerId, regionId, name, address1, address2,
    city, zip, isDefaultShipping, phoneNumber, deletedAt) <> ((Address.apply _).tupled, Address.unapply)

  def region = foreignKey(Regions.tableName, regionId, Regions)(_.id)
}

object Addresses extends TableQueryWithId[Address, Addresses](
  idLens = GenLens[Address](_.id)
  )(new Addresses(_)) {

  import scope._

  def sortedAndPagedWithRegions(query: Query[(Addresses, Regions), (Address, Region), Seq])
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage):
  QueryWithMetadata[(Addresses, Regions), (Address, Region), Seq] =
    query.withMetadata.sortAndPageIfNeeded { case (s, (address, region)) ⇒
      s.sortColumn match {
        case "id"                  ⇒ if(s.asc) address.id.asc                 else address.id.desc
        case "regionId"            ⇒ if(s.asc) address.regionId.asc           else address.regionId.desc
        case "name"                ⇒ if(s.asc) address.name.asc               else address.name.desc
        case "address1"            ⇒ if(s.asc) address.address1.asc           else address.address1.desc
        case "address2"            ⇒ if(s.asc) address.address2.asc           else address.address2.desc
        case "city"                ⇒ if(s.asc) address.city.asc               else address.city.desc
        case "zip"                 ⇒ if(s.asc) address.zip.asc                else address.zip.desc
        case "isDefaultShipping"   ⇒ if(s.asc) address.isDefaultShipping.asc  else address.isDefaultShipping.desc
        case "phoneNumber"         ⇒ if(s.asc) address.phoneNumber.asc        else address.phoneNumber.desc
        case "deletedAt"           ⇒ if(s.asc) address.deletedAt.asc          else address.deletedAt.desc
        case "region_id"           ⇒ if(s.asc) region.id.asc                  else region.id.desc
        case "region_countryId"    ⇒ if(s.asc) region.countryId.asc           else region.countryId.desc
        case "region_name"         ⇒ if(s.asc) region.name.asc                else region.name.desc
        case "region_abbreviation" ⇒ if(s.asc) region.abbreviation.asc        else region.abbreviation.desc
        case other                 ⇒ invalidSortColumn(other)
      }
    }


  def findAllByCustomerId(customerId: Int): QuerySeq =
    filter(_.customerId === customerId)

  /**
   * Return all addresses except the deleted ones.
   */
  def findAllVisibleByCustomerId(customerId: Int): QuerySeq =
    findAllByCustomerId(customerId).filter(_.deletedAt.isEmpty)

  def findAllByCustomerIdWithRegions(customerId: Int): Query[(Addresses, Regions), (Address, Region), Seq] = for {
    (addresses, regions) ← findAllByCustomerId(customerId).withRegions
  } yield (addresses, regions)

  def findAllVisibleByCustomerIdWithRegions(customerId: Int): Query[(Addresses, Regions), (Address, Region), Seq] = for {
    (addresses, regions) ← findAllVisibleByCustomerId(customerId).withRegions
  } yield (addresses, regions)

  def findShippingDefaultByCustomerId(customerId: Int): QuerySeq =
   filter(_.customerId === customerId).filter(_.isDefaultShipping === true)

  def findById(customerId: Int, addressId: Int): QuerySeq = 
   findById(addressId).extract.filter(_.customerId === customerId)

  object scope {
    implicit class AddressesQuerySeqConversions(q: QuerySeq) {
      def withRegions: Query[(Addresses, Regions), (Address, Region), Seq] = for {
        addresses ← q
        regions ← Regions if regions.id === addresses.regionId
      } yield (addresses, regions)
    }
  }
}
