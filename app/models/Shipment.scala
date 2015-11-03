package models

import com.pellucid.sealerate
import models.Shipment.Cart
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.{ADT, GenericTable, ModelWithIdParameter, TableQueryWithId}
import utils.Slick.implicits._

final case class Shipment(id: Int = 0, orderId: Int, shippingMethodId: Option[Int] = None, shippingAddressId:
Option[Int] = None, status: Shipment.Status = Cart, shippingPrice: Option[Int] = None)
  extends ModelWithIdParameter[Shipment]

object Shipment {
  sealed trait Status
  case object Cart extends Status
  case object Ordered extends Status
  case object FraudHold extends Status //this only applies at the order_header level
  case object RemorseHold extends Status //this only applies at the order_header level
  case object ManualHold extends Status //this only applies at the order_header level
  case object Canceled extends Status
  case object FulfillmentStarted extends Status
  case object PartiallyShipped extends Status
  case object Shipped extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  implicit val statusColumnType: JdbcType[Status] with BaseTypedType[Status] = Status.slickColumn
}

class Shipments(tag: Tag) extends GenericTable.TableWithId[Shipment](tag, "shipments")  {
  def id = column[Int]("id", O.PrimaryKey)
  def orderId = column[Int]("order_id")
  def shippingMethodId = column[Option[Int]]("shipping_method_id")
  def shippingAddressId = column[Option[Int]]("shipping_address_id") //Addresses table
  def status = column[Shipment.Status]("status")
  def shippingPrice = column[Option[Int]]("shipping_price") //gets filled in upon checkout

  def * = (id, orderId, shippingMethodId, shippingAddressId, status, shippingPrice) <> ((Shipment.apply _).tupled, Shipment.unapply)
}

object Shipments extends TableQueryWithId[Shipment, Shipments](
  idLens = GenLens[Shipment](_.id)
)(new Shipments(_)) {

}
