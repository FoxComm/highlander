package models.order

import java.time.Instant

import models.location.{Address, Addresses, Region, Regions}
import models.traits.Addressable
import shapeless._
import payloads.UpdateAddressPayload
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

case class OrderShippingAddress(id: Int = 0, orderId: Int = 0, regionId: Int, name: String,
  address1: String, address2: Option[String], city: String, zip: String, phoneNumber: Option[String],
  createdAt: Instant = Instant.now, updatedAt: Instant = Instant.now)
  extends FoxModel[OrderShippingAddress]
  with Addressable[OrderShippingAddress] {

  def instance: OrderShippingAddress = { this }
  def zipLens = lens[OrderShippingAddress].zip
  override def sanitize = super.sanitize(this)
  override def validate = super.validate
}

object OrderShippingAddress {
  def buildFromAddress(address: Address): OrderShippingAddress =
    OrderShippingAddress(regionId = address.regionId, name = address.name, address1 = address.address1,
      address2 = address.address2, city = address.city, zip = address.zip, phoneNumber = address.phoneNumber)

  def fromPatchPayload(a: OrderShippingAddress, p: UpdateAddressPayload) = {
    OrderShippingAddress(
      id = a.id,
      orderId = a.orderId,
      regionId = p.regionId.getOrElse(a.regionId),
      name = p.name.getOrElse(a.name),
      address1 = p.address1.getOrElse(a.address1),
      address2 = p.address2.fold(a.address2)(Some(_)),
      city = p.city.getOrElse(a.city),
      zip = p.zip.getOrElse(a.zip),
      phoneNumber = p.phoneNumber.fold(a.phoneNumber)(Some(_))
    )
  }
}

class OrderShippingAddresses(tag: Tag) extends FoxTable[OrderShippingAddress](tag, "order_shipping_addresses")
   {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def regionId = column[Int]("region_id")
  def name = column[String]("name")
  def address1 = column[String]("address1")
  def address2 = column[Option[String]]("address2")
  def city = column[String]("city")
  def zip = column[String]("zip")
  def phoneNumber = column[Option[String]]("phone_number")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")

  def * = (id, orderId, regionId, name, address1, address2, city, zip, phoneNumber,
    createdAt, updatedAt) <> ((OrderShippingAddress.apply _).tupled, OrderShippingAddress.unapply)

  def address = foreignKey(Addresses.tableName, id, Addresses)(_.id)
  def order = foreignKey(Orders.tableName, orderId, Orders)(_.id)
  def region = foreignKey(Regions.tableName, regionId, Regions)(_.id)
}

object OrderShippingAddresses
  extends FoxTableQuery[OrderShippingAddress, OrderShippingAddresses](new OrderShippingAddresses(_))
  with ReturningId[OrderShippingAddress, OrderShippingAddresses] {

  val returningLens: Lens[OrderShippingAddress, Int] = lens[OrderShippingAddress].id

  import scope._

  def copyFromAddress(address: Address, orderId: Int)(implicit ec: EC): DbResult[OrderShippingAddress] =
    create(OrderShippingAddress.buildFromAddress(address).copy(orderId = orderId))

  def findByOrderId(orderId: Int): QuerySeq =
    filter(_.orderId === orderId)

  def findByOrderIdWithRegions(orderId: Int):
  Query[(OrderShippingAddresses, Regions), (OrderShippingAddress, Region), Seq] =
    findByOrderId(orderId).withRegions

  object scope {
    implicit class OrderShippingAddressesQueryConversions(q: QuerySeq) {
      def withRegions: Query[(OrderShippingAddresses, Regions), (OrderShippingAddress, Region), Seq] = for {
        shippingAddresses ← q
        regions ← Regions if regions.id === shippingAddresses.regionId
      } yield (shippingAddresses, regions)
    }
  }
}
