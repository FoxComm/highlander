package models

import monocle.macros.GenLens
import utils.RichTable
import utils.{ GenericTable, TableQueryWithId, ModelWithIdParameter }
import scala.concurrent.{ExecutionContext, Future}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

final case class InventorySummary(id: Int, skuId: Int, availableOnHand: Int, availablePreOrder: Int, availableBackOrder: Int,
                            outstandingPreOrders: Int, outstandingBackOrders: Int) extends ModelWithIdParameter

class InventorySummaries(tag: Tag)
  extends GenericTable.TableWithId[InventorySummary](tag, "inventory_summaries") with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId = column[Int]("sku_id")
  def availableOnHand = column[Int]("available_on_hand")
  def availablePreOrder = column[Int]("available_pre_order")
  def availableBackOrder = column[Int]("available_back_order")
  def outstandingPreOrders =  column[Int]("outstanding_pre_orders") // How many have been preordered but not yet fulfilled
  def outstandingBackOrders = column[Int]("outstanding_back_orders") // How many unreconciled backorders are there.

  def * = (id, skuId, availableOnHand, availablePreOrder, availableBackOrder,
    outstandingPreOrders, outstandingBackOrders) <> (( InventorySummary.apply _).tupled, InventorySummary.unapply)
}

object InventorySummaries extends TableQueryWithId[InventorySummary, InventorySummaries](
  idLens = GenLens[InventorySummary](_.id)
)(new InventorySummaries(_)) {

  def findBySkuId(id: Int)(implicit db: Database): Future[Option[InventorySummary]] =
    db.run(_findBySkuId(id).result.headOption)

  def _findBySkuId(id: Int): Query[InventorySummaries, InventorySummary, Seq] =
    filter(_.skuId === id)
}
