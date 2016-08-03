package models.vendor

import java.time.Instant

import cats.data.Xor
import cats.data.Xor.{left, right}
import failures.{Failures, NotFoundFailure404}
import models.traits.Addressable
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.Validation
import utils.db._
import models.location._

case class VendorAddress(id: Int = 0,
                         vendorId: Int,
                         regionId: Int,
                         name: String,
                         address1: String,
                         address2: Option[String],
                         city: String,
                         zip: String,
                         isHeadquarters: Boolean = false,
                         phoneNumber: Option[String] = None,
                         deletedAt: Option[Instant] = None)
    extends FoxModel[VendorAddress]
    with Addressable[VendorAddress]
    with Validation[VendorAddress] {

  def instance: VendorAddress = { this }
  def zipLens                 = lens[VendorAddress].zip
  override def sanitize       = super.sanitize(this)
  override def validate       = super.validate
}

object VendorAddress {
  val zipPattern   = "(?i)^[a-z0-9][a-z0-9\\- ]{0,10}[a-z0-9]$"
  val zipPatternUs = "^\\d{5}(?:\\d{4})?$"
}

class VendorAddresses(tag: Tag) extends FoxTable[VendorAddress](tag, "vendorAddresses") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def vendorId       = column[Int]("vendor_id")
  def regionId       = column[Int]("region_id")
  def name           = column[String]("name")
  def address1       = column[String]("address1")
  def address2       = column[Option[String]]("address2")
  def city           = column[String]("city")
  def zip            = column[String]("zip")
  def isHeadquarters = column[Boolean]("is_headquarters")
  def phoneNumber    = column[Option[String]]("phone_number")
  def deletedAt      = column[Option[Instant]]("deleted_at")

  def * =
    (id,
     vendorId,
     regionId,
     name,
     address1,
     address2,
     city,
     zip,
     isHeadquarters,
     phoneNumber,
     deletedAt) <> ((VendorAddress.apply _).tupled, VendorAddress.unapply)

  def region = foreignKey(Regions.tableName, regionId, Regions)(_.id)
}

object VendorAddresses
    extends FoxTableQuery[VendorAddress, VendorAddresses](new VendorAddresses(_))
    with ReturningId[VendorAddress, VendorAddresses] {

  val returningLens: Lens[VendorAddress, Int] = lens[VendorAddress].id

  import scope._

  type VendorAddressesWithRegionsQuery =
    Query[(VendorAddresses, Regions), (VendorAddress, Region), Seq]

  object scope {
    implicit class VendorAddressesQuerySeqConversions(q: QuerySeq) {
      def withRegions: VendorAddressesWithRegionsQuery =
        for {
          addresses ← q
          regions   ← Regions if regions.id === addresses.regionId
        } yield (addresses, regions)
    }
  }

}
