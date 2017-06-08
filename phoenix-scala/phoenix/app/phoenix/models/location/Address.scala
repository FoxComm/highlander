package phoenix.models.location

import java.time.Instant

import cats.implicits._
import core.db._
import core.failures.{Failures, NotFoundFailure404}
import core.utils.Validation
import phoenix.models.cord.Cords
import phoenix.models.location.Addresses.scope
import phoenix.models.payment.creditcard.CreditCard
import phoenix.models.traits.Addressable
import phoenix.payloads.AddressPayloads.{CreateAddressPayload, UpdateAddressPayload}
import shapeless._
import slick.jdbc.PostgresProfile.api._

case class Address(id: Int = 0,
                   accountId: Int,
                   regionId: Int,
                   name: String,
                   address1: String,
                   address2: Option[String],
                   city: String,
                   zip: String,
                   isDefaultShipping: Boolean = false,
                   cordRef: Option[String] = None,
                   phoneNumber: Option[String] = None,
                   deletedAt: Option[Instant] = None)
    extends FoxModel[Address]
    with Addressable[Address]
    with Validation[Address] {

  def zipLens = lens[Address].zip

  override def sanitize = super.sanitize(this)
  override def validate = super.validate

  def mustBelongToAccount(accountId: Int): Either[Failures, Address] =
    if (this.isNew || this.accountId == accountId) Either.right(this)
    else Either.left(NotFoundFailure404(Address, this.id).single)

  def boundToCart(cartRef: String)(implicit ec: EC): DbResultT[Address] =
    Addresses.update(this, this.copy(cordRef = cartRef.some))

  def unboundFromCart()(implicit ec: EC): DbResultT[Address] =
    Addresses.update(this, this.copy(cordRef = None))
}

object Address {
  val zipPattern   = "(?i)^[a-z0-9][a-z0-9\\- ]{0,10}[a-z0-9]$"
  val zipPatternUs = "^\\d{5}(?:\\d{4})?$"

  def fromPayload(p: CreateAddressPayload, accountId: Int, cordRef: Option[String]) =
    Address(
      accountId = accountId,
      regionId = p.regionId,
      name = p.name,
      address1 = p.address1,
      address2 = p.address2,
      city = p.city,
      zip = p.zip,
      phoneNumber = p.phoneNumber,
      cordRef = cordRef
    )

  def fromPatchPayload(existedAddress: Address, incomingPayload: UpdateAddressPayload): Address =
    Address(
      accountId = existedAddress.accountId,
      regionId = incomingPayload.regionId.getOrElse(existedAddress.regionId),
      name = incomingPayload.name.getOrElse(existedAddress.name),
      address1 = incomingPayload.address1.getOrElse(existedAddress.address1),
      address2 = incomingPayload.address2.fold(existedAddress.address2)(Some(_)),
      city = incomingPayload.city.getOrElse(existedAddress.city),
      zip = incomingPayload.zip.getOrElse(existedAddress.zip),
      cordRef = existedAddress.cordRef,
      phoneNumber = incomingPayload.phoneNumber.fold(existedAddress.phoneNumber)(Some(_))
    )

  def fromCreditCard(cc: CreditCard): Address =
    Address(
      accountId = 0,
      regionId = cc.address.regionId,
      name = cc.address.name,
      address1 = cc.address.address1,
      address2 = cc.address.address2,
      city = cc.address.city,
      zip = cc.address.zip,
      phoneNumber = cc.address.phoneNumber,
      cordRef = None
    )

  import scope._
  def mustFindByAddressId(id: Int)(implicit ec: EC): DbResultT[(Address, Region)] =
    Addresses.findById(id).extract.withRegions.mustFindOneOr(NotFoundFailure404(Address, id))

  def mustFindByCordRef(cordRef: String)(implicit ec: EC): DbResultT[(Address, Region)] =
    Addresses
      .filter(_.cordRef === cordRef)
      .withRegions
      .mustFindOneOr(NotFoundFailure404(Address, cordRef))
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
  def cordRef           = column[Option[String]]("cord_ref")
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
     cordRef,
     phoneNumber,
     deletedAt) <> ((Address.apply _).tupled, Address.unapply)

  def region = foreignKey(Regions.tableName, regionId, Regions)(_.id)
  def cord   = foreignKey(Cords.tableName, cordRef, Cords)(_.referenceNumber)
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

  def findByOrderRef(cordRef: String): QuerySeq =
    filter(_.cordRef === cordRef)

  def findByOrderRefWithRegions(cordRef: String): AddressesWithRegionsQuery =
    findByOrderRef(cordRef).withRegions

  def findShippingDefaultByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId).filter(_.isDefaultShipping === true)

  def findByIdAndAccount(addressId: Int, accountId: Int): QuerySeq =
    findById(addressId).extract.filter(_.accountId === accountId)

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
