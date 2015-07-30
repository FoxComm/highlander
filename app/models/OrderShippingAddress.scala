package models

import scala.concurrent.ExecutionContext

import com.wix.accord.dsl.{validator => createValidator, _}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.GenericTable.TableWithId
import utils.{Validation, ModelWithIdParameter, RichTable, TableQueryWithId}

final case class OrderShippingAddress(id: Int = 0, orderId: Int = 0, stateId: Int, name: String,
  street1: String, street2: Option[String], city: String, zip: String)
  extends Validation[OrderShippingAddress]
  with ModelWithIdParameter {

  override def validator = createValidator[OrderShippingAddress] { address =>
    address.name is notEmpty
    address.street1 is notEmpty
    address.city is notEmpty
    address.zip should matchRegex("[0-9]{5}")
  }
}

object OrderShippingAddress {
  def buildFromAddress(address: Address): OrderShippingAddress =
    OrderShippingAddress(stateId = address.stateId, name = address.name, street1 = address.street1,
      street2 = address.street2, city = address.city, zip = address.zip)
}

class OrderShippingAddresses(tag: Tag) extends TableWithId[OrderShippingAddress](tag, "order_shipping_addresses")
  with RichTable {
  def id = column[Int]("id", O.PrimaryKey)
  def orderId = column[Int]("order_id")
  def stateId = column[Int]("state_id")
  def name = column[String]("name")
  def street1 = column[String]("street1")
  def street2 = column[Option[String]]("street2")
  def city = column[String]("city")
  def zip = column[String]("zip")

  def * = (id, orderId, stateId, name, street1, street2,
    city, zip) <> ((OrderShippingAddress.apply _).tupled, OrderShippingAddress.unapply)

  def address = foreignKey(Addresses.tableName, id, Addresses)(_.id)
  def order = foreignKey(Orders.tableName, orderId, Orders)(_.id)
  def state = foreignKey(States.tableName, stateId, States)(_.id)
}

object OrderShippingAddresses extends TableQueryWithId[OrderShippingAddress, OrderShippingAddresses](
  idLens = GenLens[OrderShippingAddress](_.id)
)(new OrderShippingAddresses(_)) {

  def copyFromAddress(address: Address, orderId: Int)(implicit ec: ExecutionContext):
  DBIO[OrderShippingAddress] =
    save(OrderShippingAddress.buildFromAddress(address).copy(orderId = orderId))

//  def create(address: OrderShippingAddress)(implicit ec: ExecutionContext): DBIO[OrderShippingAddress] = for {
//    shippingAddress <- save(address)
//  } yield shippingAddress
//
//  def findByOrderId(orderId: Int):
//  Query[(Addresses, OrderShippingAddresses), (Address, OrderShippingAddress), Seq] = for {
//    addresses ← Addresses._findAllByCustomerId(customerId)
//    shippingAddresses ← this if shippingAddresses.id === addresses.id
//  } yield (addresses, shippingAddresses)
//
//  def findAllByCustomerIdWithStates(customerId: Int):
//  Query[(Addresses, OrderShippingAddresses, States), (Address, OrderShippingAddress, State), Seq] = for {
//    records ← withStates(findAllByCustomerId(customerId))
//  } yield records
//
//  def withStates(q: Query[(Addresses, OrderShippingAddresses), (Address, OrderShippingAddress), Seq]):
//  Query[(Addresses, OrderShippingAddresses, States), (Address, OrderShippingAddress, State), Seq] = for {
//    (addresses, shippingAddresses) ← q
//    states ← States if states.id === addresses.id
//  } yield (addresses, shippingAddresses, states)
}
