package phoenix.responses

import java.time.Instant

import cats.implicits._
import core.db._
import phoenix.models.account._
import phoenix.models.location._
import phoenix.models.payment.creditcard.CreditCard

case class AddressResponse(id: Int,
                           customer: Option[User] = None,
                           region: Region,
                           name: String,
                           address1: String,
                           address2: Option[String] = None,
                           city: String,
                           zip: String,
                           isDefault: Option[Boolean] = None,
                           phoneNumber: Option[String] = None,
                           deletedAt: Option[Instant] = None)
    extends ResponseItem

object AddressResponse {

  def fromAddressId(id: Int)(implicit ec: EC, db: DB): DbResultT[AddressResponse] =
    for {
      address ← * <~ Addresses.mustFindById400(id)
      region  ← * <~ Regions.mustFindById400(address.regionId)
    } yield build(address, region)

  def fromAddress(address: Address)(implicit ec: EC, db: DB): DbResultT[AddressResponse] =
    Regions.mustFindById400(address.regionId).map(region ⇒ build(address, region))

  def build(address: Address, region: Region): AddressResponse =
    AddressResponse(
      id = address.id,
      region = region,
      name = address.name,
      address1 = address.address1,
      address2 = address.address2,
      city = address.city,
      zip = address.zip,
      isDefault = address.isDefaultShipping.some,
      phoneNumber = address.phoneNumber,
      deletedAt = address.deletedAt
    )

  def buildFromCreditCard(cc: CreditCard, region: Region): AddressResponse =
    AddressResponse(
      id = 0,
      region = region,
      name = cc.address.name,
      address1 = cc.address.address1,
      address2 = cc.address.address2,
      city = cc.address.city,
      zip = cc.address.zip,
      isDefault = None,
      phoneNumber = cc.address.phoneNumber
    )

  def buildMulti(records: Seq[(Address, Region)]): Seq[AddressResponse] =
    records.map((build _).tupled)

  def forCordRef(cordRef: String)(implicit ec: EC): DbResultT[AddressResponse] =
    for {
      fullAddress ← * <~ Address.mustFindByCordRef(cordRef)
      (address, region) = fullAddress
    } yield build(address, region)
}
