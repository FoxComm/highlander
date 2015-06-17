package models

import utils.RichTable
import utils.{ GenericTable, TableQueryWithId, ModelWithIdParameter }

import scala.concurrent.{ExecutionContext, Future}


class InventorySummary {

}

class InventorySummaries(tag: Tag) extends GenericTable.TableWithId[InventorySummary](tag, "inventory_summaries") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId = column[Int]("sku_id")
  def availableOnHand = column[Int]("available_on_hand")
  def availablePreOrder = column[Int]("available_pre_order")
  def availableBackOrder = column[Int]("available_pre_order")
  def outstandingPreOrders =  column[Int]("available_pre_order") // How many have been preordered but not yet fulfilled
  def outstandingBackOrders = column[Int]("available_pre_order") // How many unreconciled backorders are there.
}