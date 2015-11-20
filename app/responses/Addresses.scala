package responses

import java.time.Instant
import scala.concurrent.ExecutionContext

import models.{CreditCard, OrderShippingAddresses, Address, Customer, OrderShippingAddress, Region}
import services.NotFoundFailure404
import utils.Slick.DbResult
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object Addresses {
  final case class Root(id: Int, customer: Option[Customer] = None, region: Region, name: String, address1: String,
    address2: Option[String] = None, city: String, zip: String, isDefault: Option[Boolean] = None,
    phoneNumber: Option[String] = None, deletedAt: Option[Instant] = None) extends ResponseItem

  def build(address: Address, region: Region, isDefault: Option[Boolean] = None): Root =
    Root(id = address.id, region = region, name = address.name, address1 = address.address1, address2 = address.address2,
      city = address.city, zip = address.zip, isDefault = isDefault, phoneNumber = address.phoneNumber, deletedAt = address.deletedAt)

  def buildFromCreditCard(cc: CreditCard, region: Region): Root =
    Root(id = 0, region = region, name = cc.name, address1 = cc.address1, address2 = cc.address2,
      city = cc.city, zip = cc.zip, isDefault = None, phoneNumber = cc.phoneNumber)

  def build(records: Seq[(models.Address, Region)]): Seq[Root] =
    records.map { case (address, region) ⇒ build(address, region, Some(address.isDefaultShipping)) }

  def buildShipping(records: Seq[(models.Address, OrderShippingAddress, Region)]): Seq[Root] = {
    records.map { case (address, shippingAddress, region) ⇒
      build(address, region)
    }
  }

  def buildOneShipping(address: OrderShippingAddress, region: Region, isDefault: Boolean = false): Root = {
    Root(id = address.id, region = region, name = address.name, address1 = address.address1, address2 = address.address2,
      city = address.city, zip = address.zip, isDefault = Some(isDefault), phoneNumber = address.phoneNumber, deletedAt = None)
  }

  def forOrderId(orderId: Int)(implicit ec: ExecutionContext): DbResult[Root] = {
    val fullAddressDetails = for {
      shipAddress ← OrderShippingAddresses.findByOrderId(orderId)
      region ← shipAddress.region
    } yield (shipAddress, region)

    fullAddressDetails.result.flatMap { res ⇒ val (addresses, regions) = res.unzip
      (addresses.headOption, regions.headOption) match {
        case (Some(address), Some(region)) ⇒ DbResult.good(buildOneShipping(address, region))
        case (None, _) ⇒ DbResult.failure(NotFoundFailure404(s"No addresses found for order with id=$orderId"))
        case (Some(address), None) ⇒ DbResult.failure(NotFoundFailure404(Region, address.regionId))
      }
    }
  }
}

