package models

import utils.RichTable
import utils.{ GenericTable, TableQueryWithId, ModelWithIdParameter }
import monocle.macros.GenLens
import scala.concurrent.{ExecutionContext, Future}
import slick.driver.PostgresDriver.api._

final case class InventoryClassification(id: Int, skuId: Int, canSell: Boolean,
                                   canPreOrder: Boolean, canBackOrder: Boolean) extends ModelWithIdParameter

class InventoryClassifications(tag: Tag)
  extends GenericTable.TableWithId[InventoryClassification](tag, "inventory_classifications") with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId = column[Int]("sku_id")
  def canSell = column[Boolean]("can_sell")
  def canPreOrder = column[Boolean]("can_pre_order")
  def canBackOrder = column[Boolean]("can_back_order")

  def * = (id, skuId, canSell, canPreOrder, canBackOrder) <> ((InventoryClassification.apply _).tupled, InventoryClassification.unapply)
}

object InventoryClassifications extends TableQueryWithId[InventoryClassification, InventoryClassifications](
  idLens = GenLens[InventoryClassification](_.id)
)(new InventoryClassifications(_))
