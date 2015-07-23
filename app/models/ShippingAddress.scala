package models

import scala.concurrent.ExecutionContext

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.GenericTable.TableWithId
import utils.{ModelWithIdParameter, RichTable, TableQueryWithId}

final case class ShippingAddress(id: Int = 0, customerId: Int = 0, isDefault: Boolean = false)
  extends ModelWithIdParameter

class ShippingAddresses(tag: Tag) extends TableWithId[ShippingAddress](tag, "shipping_addresses") with RichTable {
  def id = column[Int]("id", O.PrimaryKey)
  def customerId = column[Int]("customer_id")
  def isDefault = column[Boolean]("is_default")

  def * = (id, customerId, isDefault) <> ((ShippingAddress.apply _).tupled, ShippingAddress.unapply)

  def address = foreignKey("shipping_addresses_address_id_fkey", id, TableQuery[Addresses])(_.id)
  def customer = foreignKey("shipping_addresses_address_id_fkey", customerId, TableQuery[Addresses])(_.id)
}

object ShippingAddresses extends TableQueryWithId[ShippingAddress, ShippingAddresses](
  idLens = GenLens[ShippingAddress](_.id)
)(new ShippingAddresses(_)) {

  def createFromAddress(address: Address, isDefault: Boolean = false)
    (implicit ec: ExecutionContext): DBIO[(Address, ShippingAddress)] = for {
    newAddress <- Addresses.save(address)
    shippingAddress <- save(ShippingAddress(id = newAddress.id, customerId = address.customerId,
      isDefault = isDefault))
  } yield (newAddress, shippingAddress)

  def create(address: ShippingAddress)(implicit ec: ExecutionContext): DBIO[ShippingAddress] = for {
    shippingAddress <- save(address)
  } yield shippingAddress

  def findAllByCustomerId(customerId: Int): Query[(Addresses, ShippingAddresses), (Address, ShippingAddress), Seq] =
    for {
    addresses ← Addresses._findAllByCustomerId(customerId)
    shippingAddresses ← this if shippingAddresses.id === addresses.id
  } yield (addresses, shippingAddresses)
}
