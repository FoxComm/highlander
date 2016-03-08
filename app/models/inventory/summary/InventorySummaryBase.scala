package models.inventory.summary

import java.time.Instant
import models.javaTimeSlickMapper
import monocle.Lens
import slick.driver.PostgresDriver.api._
import slick.lifted.{Query, Tag}
import utils.Slick.DbResult
import utils.aliases.EC
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

trait InventorySummaryBase[A <: ModelWithIdParameter[A]] extends ModelWithIdParameter[A] { self: A ⇒
  def onHand: Int
  def onHold: Int
  def reserved: Int
  def availableForSale: Int // set by DB trigger
  def updatedAt: Instant

  def availableForSaleCost(price: Int): Int = availableForSale * price
}

abstract class InventorySummariesTableBase[A <: InventorySummaryBase[A]](tag: Tag, tableName: String)
  extends GenericTable.TableWithId[A](tag, tableName) {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def onHand = column[Int]("on_hand")
  def onHold = column[Int]("on_hold")
  def reserved = column[Int]("reserved")
  def availableForSale = column[Int]("available_for_sale")
  def updatedAt = column[Instant]("updated_at")
}

abstract class InventorySummariesBase[A <: InventorySummaryBase[A], As <: InventorySummariesTableBase[A]]
(idLens: Lens[A, A#Id])(construct: Tag ⇒ As)
  extends TableQueryWithId[A, As](idLens)(construct) {

  override type QuerySeq = Query[As, A, Seq]

  val returningIdAndAfs = this.returning(map { o ⇒ (o.id, o.availableForSale) })

  // Can't provide generic implementation with A
  def returningAction(ret: (Int, Int))(summary: A): A

  override def create[R](summary: A, returning: Returning[R], action: R ⇒ A ⇒ A)(implicit ec: EC): DbResult[A] =
    super.create(summary, returningIdAndAfs, returningAction)
}
