package models


import utils.RichTable
import utils.{ GenericTable, TableQueryWithId, ModelWithIdParameter }

import scala.concurrent.{ExecutionContext, Future}


class InventoryClassification {

}


class InventoryClassifications(tag: Tag) extends GenericTable.TableWithId[InventoryClassification](tag, "inventory_classifications") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId = column[Int]("sku_id")
  def availableForSale = column[Boolean]("available_for_sale")
  def canPreOrder = column[Boolean]("can_pre_order")
  def canBackOrder = column[Boolean]("can_back_order")
}
