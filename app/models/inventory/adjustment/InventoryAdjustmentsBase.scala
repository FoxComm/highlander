package models.inventory.adjustment

import java.time.Instant

import models.javaTimeSlickMapper
import monocle.Lens
import org.json4s.JsonAST.JValue
import slick.lifted.{Query, Tag}
import utils.ExPostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

trait InventoryAdjustmentBase[A <: ModelWithIdParameter[A]] extends ModelWithIdParameter[A] { self: A ⇒
  def summaryId: Int
  def onHandChange: Int
  def onHoldChange: Int
  def reservedChange: Int
  def metadata: JValue
  def createdAt: Instant
}

abstract class InventoryAdjustmentsTableBase[A <: InventoryAdjustmentBase[A]](tag: Tag, tableName: String)
  extends GenericTable.TableWithId[A](tag, tableName) {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def summaryId = column[Int]("summary_id")
  def onHandChange = column[Int]("on_hand_change")
  def onHoldChange = column[Int]("on_hold_change")
  def reservedChange = column[Int]("reserved_change")
  def metadata = column[JValue]("metadata")
  def createdAt = column[Instant]("created_at")
}

abstract class InventoryAdjustmentsBase[A <: InventoryAdjustmentBase[A], As <: InventoryAdjustmentsTableBase[A]]
(idLens: Lens[A, A#Id])(construct: Tag ⇒ As)
  extends TableQueryWithId[A, As](idLens)(construct) {

  override type QuerySeq = Query[As, A, Seq]

  def findBySummaryId(summaryId: Int): QuerySeq =
    filter(_.summaryId === summaryId)
}
