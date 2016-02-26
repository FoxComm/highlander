package models.inventory.summary

import monocle.Lens
import slick.driver.PostgresDriver.api._
import slick.lifted.{Query, Tag}
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

trait InventorySummaryBase[A <: ModelWithIdParameter[A]] extends ModelWithIdParameter[A] { self: A ⇒
  def onHand: Int
  def onHold: Int
  def reserved: Int

  def availableForSale: Int = onHand - onHold - reserved
  def availableForSaleCost(price: Int): Int = availableForSale * price
}

abstract class InventorySummariesTableBase[A <: InventorySummaryBase[A]](tag: Tag, tableName: String)
  extends GenericTable.TableWithId[A](tag, tableName) {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def onHand = column[Int]("on_hand")
  def onHold = column[Int]("on_hold")
  def reserved = column[Int]("reserved")
}

abstract class InventorySummariesBase[A <: InventorySummaryBase[A], As <: InventorySummariesTableBase[A]]
(idLens: Lens[A, A#Id])(construct: Tag ⇒ As)
  extends TableQueryWithId[A, As](idLens)(construct) {

  override type QuerySeq = Query[As, A, Seq]

}
