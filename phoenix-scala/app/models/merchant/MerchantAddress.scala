package models.merchant

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

case class MerchantAddress(id: Int = 0,
                           merchantId: Int,
                           regionId: Int,
                           name: String,
                           address1: String,
                           address2: Option[String],
                           city: String,
                           zip: String,
                           isHeadquarters: Boolean = false,
                           phoneNumber: Option[String] = None,
                           deletedAt: Option[Instant] = None)
    extends FoxModel[MerchantAddress]
    with Addressable[MerchantAddress]
    with Validation[MerchantAddress] {

  def instance: MerchantAddress = { this }
  def zipLens                   = lens[MerchantAddress].zip
  override def sanitize         = super.sanitize(this)
  override def validate         = super.validate
}

object MerchantAddress {
  val zipPattern   = "(?i)^[a-z0-9][a-z0-9\\- ]{0,10}[a-z0-9]$"
  val zipPatternUs = "^\\d{5}(?:\\d{4})?$"
}

class MerchantAddresses(tag: Tag) extends FoxTable[MerchantAddress](tag, "merchantAddresses") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def merchantId     = column[Int]("merchant_id")
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
     merchantId,
     regionId,
     name,
     address1,
     address2,
     city,
     zip,
     isHeadquarters,
     phoneNumber,
     deletedAt) <> ((MerchantAddress.apply _).tupled, MerchantAddress.unapply)

  def region = foreignKey(Regions.tableName, regionId, Regions)(_.id)
}

object MerchantAddresses
    extends FoxTableQuery[MerchantAddress, MerchantAddresses](new MerchantAddresses(_))
    with ReturningId[MerchantAddress, MerchantAddresses] {

  val returningLens: Lens[MerchantAddress, Int] = lens[MerchantAddress].id

  import scope._

  type MerchantAddressesWithRegionsQuery =
    Query[(MerchantAddresses, Regions), (MerchantAddress, Region), Seq]

  object scope {
    implicit class MerchantAddressesQuerySeqConversions(q: QuerySeq) {
      def withRegions: MerchantAddressesWithRegionsQuery =
        for {
          addresses ← q
          regions   ← Regions if regions.id === addresses.regionId
        } yield (addresses, regions)
    }
  }

}
