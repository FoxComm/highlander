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

case class OrderLineItem(id: Int = 0, orderId: Int, skuId: Int, status: OrderLineItem.Status)

object OrderLineItem{
  sealed trait Status
  case object New extends Status
  case object Canceled extends Status
  case object ProductionStarted extends Status
  case object PostProductionStarted extends Status // can include creating, customizing, etc. eg. engraving
  case object FulfillmentStarted extends Status
  case object PartiallyShipped extends Status // would only be relevant for a complex product with componentry
  case object Shipped extends Status

  implicit val StatusColumnType = MappedColumnType.base[Status, String]({
    case t @ (New | Canceled | ProductionStarted | PostProductionStarted | FulfillmentStarted | PartiallyShipped | Shipped) => t.toString.toLowerCase
    case unknown => throw new IllegalArgumentException(s"cannot map status column to type $unknown")
  },
  {
    case "new" => New
    case "canceled" => Canceled
    case "productionstarted" => ProductionStarted
    case "postproductionstarted" => PostProductionStarted
    case "fulfillmentstarted" => FulfillmentStarted
    case "partiallyshipped" => PartiallyShipped
    case "shipped" => Shipped
    case unknown => throw new IllegalArgumentException(s"cannot map status column to type $unknown")
  })
}


class OrderLineItems(tag: Tag) extends Table[OrderLineItem](tag, "order_line_items") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def skuId = column[Int]("sku_id")
  def status = column[OrderLineItem.Status]("status")
  def * = (id, orderId, skuId, status) <> ((OrderLineItem.apply _).tupled, OrderLineItem.unapply)
}

object OrderLineItems {
  val table = TableQuery[OrderLineItems]
  val returningId = table.returning(table.map(_.id))

  def findByOrder(order: Order)(implicit ec: ExecutionContext, db: Database) = { db.run(_findByOrderId(order.id).result) }

  def _findByOrderId(orderId: Rep[Int]) = { table.filter(_.orderId === orderId) }
}