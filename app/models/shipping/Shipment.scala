package models.shipping

import java.time.Instant

import com.pellucid.sealerate
import models.shipping.Shipment._
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.db._
import utils.ADT

case class Shipment(id: Int = 0, orderId: Int, orderShippingMethodId: Option[Int] = None, shippingAddressId:
Option[Int] = None, state: Shipment.State = Cart, shippingPrice: Option[Int] = None,
  updatedAt: Option[Instant] = Some(Instant.now))
  extends FoxModel[Shipment]

object Shipment {
  sealed trait State
  case object Cart extends State
  case object Ordered extends State
  case object FraudHold extends State //this only applies at the order_header level
  case object RemorseHold extends State //this only applies at the order_header level
  case object ManualHold extends State //this only applies at the order_header level
  case object Canceled extends State
  case object FulfillmentStarted extends State
  case object PartiallyShipped extends State
  case object Shipped extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn
}

class Shipments(tag: Tag) extends FoxTable[Shipment](tag, "shipments")  {
  def id = column[Int]("id", O.PrimaryKey)
  def orderId = column[Int]("order_id")
  def orderShippingMethodId = column[Option[Int]]("order_shipping_method_id")
  def shippingAddressId = column[Option[Int]]("shipping_address_id") //Addresses table
  def state = column[Shipment.State]("state")
  def shippingPrice = column[Option[Int]]("shipping_price") //gets filled in upon checkout
  def updatedAt = column[Option[Instant]]("updated_at")

  def * = (id, orderId, orderShippingMethodId, shippingAddressId, state, shippingPrice, updatedAt) <>
    ((Shipment.apply _).tupled, Shipment.unapply)
}

object Shipments extends FoxTableQuery[Shipment, Shipments](
  idLens = lens[Shipment].id
)(new Shipments(_)) {

}
