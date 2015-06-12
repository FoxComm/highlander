package models

import utils.{Validation, RichTable}
import payloads.CreateAddressPayload

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

case class ShippingAddress(addressId: Int)//, paymentId: Int)

class ShippingAddresses(tag: Tag) extends Table[ShippingAddress](tag, "shipping_addresses") with RichTable {
  def addressId = column[Int]("address_id", O.PrimaryKey)
//  def shipmentId = column[Int]("payment_id", O.PrimaryKey)

//  def * = (addressId, shipmentId) <> ((ShippingAddress.apply _).tupled, ShippingAddress.unapply)
  def * = addressId <> ((ShippingAddress.apply _), ShippingAddress.unapply)

  def address = foreignKey("billing_addresses_address_id_fk", addressId, TableQuery[Addresses])(_.id)
//  def payment = foreignKey("billing_addresses_applied_payments_id_fk", paymentId, TableQuery[AppliedPayments])(_.id)
}

object ShippingAddresses {
  val table = TableQuery[ShippingAddresses]
}