package models

import scala.concurrent.ExecutionContext

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.GenericTable.TableWithId
import utils.{ModelWithIdParameter, RichTable, TableQueryWithId}

final case class OrderShippingAddress(id: Int = 0, customerId: Int = 0, isDefault: Boolean = false)
  extends ModelWithIdParameter

class OrderShippingAddresses(tag: Tag) extends TableWithId[OrderShippingAddress](tag, "shipping_addresses") with RichTable {
  def id = column[Int]("id", O.PrimaryKey)
  def customerId = column[Int]("customer_id")
  def isDefault = column[Boolean]("is_default")

  def * = (id, customerId, isDefault) <> ((OrderShippingAddress.apply _).tupled, OrderShippingAddress.unapply)

  def address = foreignKey(Addresses.tableName, id, Addresses)(_.id)
  def customer = foreignKey(Customers.tableName, customerId, Customers)(_.id)
}

object OrderShippingAddresses extends TableQueryWithId[OrderShippingAddress, OrderShippingAddresses](
  idLens = GenLens[OrderShippingAddress](_.id)
)(new OrderShippingAddresses(_)) {

  def createFromAddress(address: Address, isDefault: Boolean = false)
    (implicit ec: ExecutionContext): DBIO[(Address, OrderShippingAddress)] = for {
    newAddress <- Addresses.save(address)
    shippingAddress <- save(OrderShippingAddress(id = newAddress.id, customerId = address.customerId,
      isDefault = isDefault))
  } yield (newAddress, shippingAddress)

  def create(address: OrderShippingAddress)(implicit ec: ExecutionContext): DBIO[OrderShippingAddress] = for {
    shippingAddress <- save(address)
  } yield shippingAddress

  def findAllByCustomerId(customerId: Int):
  Query[(Addresses, OrderShippingAddresses), (Address, OrderShippingAddress), Seq] = for {
    addresses ← Addresses._findAllByCustomerId(customerId)
    shippingAddresses ← this if shippingAddresses.id === addresses.id
  } yield (addresses, shippingAddresses)

  def findAllByCustomerIdWithStates(customerId: Int):
  Query[(Addresses, OrderShippingAddresses, States), (Address, OrderShippingAddress, State), Seq] = for {
    records ← withStates(findAllByCustomerId(customerId))
  } yield records

  def withStates(q: Query[(Addresses, OrderShippingAddresses), (Address, OrderShippingAddress), Seq]):
  Query[(Addresses, OrderShippingAddresses, States), (Address, OrderShippingAddress, State), Seq] = for {
    (addresses, shippingAddresses) ← q
    states ← States if states.id === addresses.id
  } yield (addresses, shippingAddresses, states)
}
