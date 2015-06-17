package models

import monocle.macros.GenLens
import utils.RichTable
import utils.{ GenericTable, TableQueryWithId, ModelWithIdParameter }
import scala.concurrent.{ExecutionContext, Future}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}


case class InventorySummary(id: Int, skuId: Int, availableOnHand: Int, availablePreOrder: Int, availableBackOrder: Int, outstandingPreOrders: Int, outstandingBackOrders: Int) extends ModelWithIdParameter {

}

class InventorySummaries(tag: Tag) extends GenericTable.TableWithId[InventorySummary](tag, "inventory_summaries") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId = column[Int]("sku_id")
  def availableOnHand = column[Int]("available_on_hand")
  def availablePreOrder = column[Int]("available_pre_order")
  def availableBackOrder = column[Int]("available_pre_order")
  def outstandingPreOrders =  column[Int]("available_pre_order") // How many have been preordered but not yet fulfilled
  def outstandingBackOrders = column[Int]("available_pre_order") // How many unreconciled backorders are there.

  def * = (id, skuId, availableOnHand, availablePreOrder, availableBackOrder, outstandingPreOrders, outstandingPreOrders) <> (( InventorySummary.apply _).tupled, InventorySummary.unapply)
}

object InventorySummaries extends TableQueryWithId[InventorySummary, InventorySummaries](
  idLens = GenLens[InventorySummary](_.id)
)(new InventorySummaries(_)) {}