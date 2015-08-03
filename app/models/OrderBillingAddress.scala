package models

import scala.concurrent.ExecutionContext

import com.wix.accord.dsl.{validator => createValidator, _}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.GenericTable.TableWithId
import utils.{Validation, ModelWithIdParameter, RichTable, TableQueryWithId}

final case class OrderBillingAddress(id: Int = 0, orderId: Int = 0, stateId: Int, name: String,
  street1: String, street2: Option[String], city: String, zip: String)
  extends Validation[OrderBillingAddress]
  with ModelWithIdParameter {

  override def validator = createValidator[OrderBillingAddress] { address =>
    address.name is notEmpty
    address.street1 is notEmpty
    address.city is notEmpty
    address.zip should matchRegex("[0-9]{5}")
  }
}

object OrderBillingAddress {
  def buildFromAddress(address: Address): OrderBillingAddress =
    OrderBillingAddress(stateId = address.stateId, name = address.name, street1 = address.street1,
      street2 = address.street2, city = address.city, zip = address.zip)
}

class OrderBillingAddresses(tag: Tag) extends TableWithId[OrderBillingAddress](tag, "order_shipping_addresses")
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
    city, zip) <> ((OrderBillingAddress.apply _).tupled, OrderBillingAddress.unapply)

  def address = foreignKey(Addresses.tableName, id, Addresses)(_.id)
  def order = foreignKey(Orders.tableName, orderId, Orders)(_.id)
  def state = foreignKey(States.tableName, stateId, States)(_.id)
}

object OrderBillingAddresses extends TableQueryWithId[OrderBillingAddress, OrderBillingAddresses](
  idLens = GenLens[OrderBillingAddress](_.id)
)(new OrderBillingAddresses(_)) {

  def copyFromAddress(address: Address, orderId: Int)(implicit ec: ExecutionContext):
  DBIO[OrderBillingAddress] =
    save(OrderBillingAddress.buildFromAddress(address).copy(orderId = orderId))
}
