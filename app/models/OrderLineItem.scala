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

case class OrderLineItem(id: Int, orderId: Int, skuId: Int)

class OrderLineItems(tag: Tag) extends Table[OrderLineItem](tag, "order_line_items") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def skuId = column[Int]("sku_id")
  def * = (id, orderId, skuId) <> ((OrderLineItem.apply _).tupled, OrderLineItem.unapply)
}

object OrderLineItems {
  val table = TableQuery[OrderLineItems]
  val returningId = table.returning(table.map(_.id))

  def findByOrder(order: Order)(implicit db: Database) = {db.run(_findByOrderId(order.id).result) }

  def _findByOrderId(orderId: Rep[Int]) = { table.filter(_.orderId === orderId) }
}