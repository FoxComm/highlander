package models

import scala.concurrent.ExecutionContext

import com.wix.accord.dsl.{validator => createValidator, _}
import monocle.macros.GenLens
import payloads.UpdateAddressPayload
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.GenericTable.TableWithId
import utils.{Validation, ModelWithIdParameter, RichTable, TableQueryWithId}

final case class OrderShippingAddress(id: Int = 0, orderId: Int = 0, stateId: Int, name: String,
  street1: String, street2: Option[String], city: String, zip: String, phoneNumber: Option[String])
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
      street2 = address.street2, city = address.city, zip = address.zip, phoneNumber = address.phoneNumber)

  def fromPatchPayload(a: OrderShippingAddress, p: UpdateAddressPayload) = {
    OrderShippingAddress(
      id = a.id,
      orderId = a.orderId,
      stateId = p.stateId.getOrElse(a.stateId),
      name = p.name.getOrElse(a.name),
      street1 = p.street1.getOrElse(a.street1),
      street2 = p.street2.fold(a.street2)(Some(_)),
      city = p.city.getOrElse(a.city),
      zip = p.zip.getOrElse(a.zip),
      phoneNumber = p.phoneNumber.fold(a.phoneNumber)(Some(_))
    )
  }
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
  def phoneNumber = column[Option[String]]("phone_number")

  def * = (id, orderId, stateId, name, street1, street2,
    city, zip, phoneNumber) <> ((OrderShippingAddress.apply _).tupled, OrderShippingAddress.unapply)

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

  def updateFromPatch(address: OrderShippingAddress, payload: UpdateAddressPayload)(implicit ec: ExecutionContext):
  DBIO[OrderShippingAddress] =
    save(OrderShippingAddress.fromPatchPayload(a = address, p = payload))

  def findByOrderId(orderId: Int): Query[OrderShippingAddresses, OrderShippingAddress, Seq] =
    filter(_.orderId === orderId)

  def findByOrderIdWithStates(orderId: Int):
  Query[(OrderShippingAddresses, States), (OrderShippingAddress, State), Seq] = for {
    records ← withStates(findByOrderId(orderId))
  } yield records

  def withStates(q: Query[(OrderShippingAddresses), (OrderShippingAddress), Seq]):
  Query[(OrderShippingAddresses, States), (OrderShippingAddress, State), Seq] = for {
    shippingAddresses ← q
    states ← States if states.id === shippingAddresses.id
  } yield (shippingAddresses, states)
}
