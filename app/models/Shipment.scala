package models

import models.Shipment.Cart
import utils.{GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}


case class Shipment(id: Int = 0, orderId: Int, shippingMethodId: Option[Int] = None, shippingAddressId: Option[Int] = None, status: Shipment.Status = Cart, shippingPrice: Option[Int] = None) extends ModelWithIdParameter

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

  implicit val StatusColumnType = MappedColumnType.base[Status, String]({
    case t => t.toString.toLowerCase
  },
  {
    case "cart" => Cart
    case "ordered" => Ordered
    case "fraudhold" => FraudHold
    case "remorsehold" => RemorseHold
    case "manualhold" => ManualHold
    case "canceled" => Canceled
    case "fulfillmen_started" => FulfillmentStarted
    case "partiallyshipped" => PartiallyShipped
    case "shipped" => Shipped
    case unknown => throw new IllegalArgumentException(s"cannot map status column to type $unknown")
  })

}

class Shipments(tag: Tag) extends GenericTable.TableWithId[Shipment](tag, "shipments") with RichTable {
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

  def findByOrderId(id: Int) = {
    _findByOrderId(id).result.headOption
  }

  def _findByOrderId(id: Int) = {
    filter(_.orderId === id)
  }

}