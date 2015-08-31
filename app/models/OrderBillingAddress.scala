package models

import scala.concurrent.ExecutionContext

import com.wix.accord.dsl.{validator => createValidator, _}
import monocle.Lens
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.GenericTable.TableWithId
import utils.{NewModel, ModelWithIdParameter, RichTable, TableQueryWithId}

final case class OrderBillingAddress(id: Int = 0, orderPaymentId: Int = 0,
  regionId: Int, name: String, street1: String, street2: Option[String], city: String, zip: String)
  extends ModelWithIdParameter
  with NewModel
  with Addressable[OrderBillingAddress] {

  def isNew: Boolean = id == 0

  def instance: OrderBillingAddress = { this }
  def zipLens = Lens[OrderBillingAddress, String](_.zip)(n ⇒ a ⇒ a.copy(zip = n))

  def phoneNumber: Option[String] = { None }
}

object OrderBillingAddress {
  def buildFromAddress(address: Address): OrderBillingAddress =
    OrderBillingAddress(regionId = address.regionId, name = address.name, street1 = address.street1,
      street2 = address.street2, city = address.city, zip = address.zip)
}

class OrderBillingAddresses(tag: Tag) extends TableWithId[OrderBillingAddress](tag, "order_billing_addresses")
with RichTable {
  def id = column[Int]("id", O.PrimaryKey)
  def orderPaymentId = column[Int]("order_payment_id")
  def regionId = column[Int]("region_id")
  def name = column[String]("name")
  def street1 = column[String]("street1")
  def street2 = column[Option[String]]("street2")
  def city = column[String]("city")
  def zip = column[String]("zip")

  def * = (id, orderPaymentId, regionId, name, street1, street2,
    city, zip) <> ((OrderBillingAddress.apply _).tupled, OrderBillingAddress.unapply)

  def address = foreignKey(Addresses.tableName, id, Addresses)(_.id)
  def region = foreignKey(Regions.tableName, regionId, Regions)(_.id)
  def orderPayment = foreignKey(OrderPayments.tableName, orderPaymentId, OrderPayments)(_.id)
}

object OrderBillingAddresses extends TableQueryWithId[OrderBillingAddress, OrderBillingAddresses](
  idLens = GenLens[OrderBillingAddress](_.id)
)(new OrderBillingAddresses(_)) {

  def copyFromAddress(address: Address, orderId: Int, orderPaymentId: Int)(implicit ec: ExecutionContext):
  DBIO[OrderBillingAddress] =
    save(OrderBillingAddress.buildFromAddress(address).copy(orderPaymentId = orderPaymentId))
}
