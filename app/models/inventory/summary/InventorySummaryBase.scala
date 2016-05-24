package models.inventory.summary

import java.time.Instant

import cats.data.ValidatedNel
import cats.implicits._
import failures.Failure
import shapeless._
import slick.driver.PostgresDriver.api._
import slick.lifted.{Query, Tag}
import utils.Validation
import utils.aliases.EC
import utils.db._

trait InventorySummaryBase[A <: FoxModel[A]] extends FoxModel[A] with Validation[A] { self: A ⇒

  def onHand: Int
  def onHold: Int
  def reserved: Int
  def availableForSale: Int // set by DB trigger
  def updatedAt: Instant

  def availableForSaleCost(price: Int): Int = availableForSale * price

  import Validation._

  override def validate: ValidatedNel[Failure, A] =
    (greaterThanOrEqual(onHand, 0, "On hand quantity") |@| greaterThanOrEqual(
            onHold, 0, "On hold quantity") |@| greaterThanOrEqual(
            reserved, 0, "Reserved quantity")).map {
      case _ ⇒ this
    }
}

abstract class InventorySummariesTableBase[A <: InventorySummaryBase[A]](
    tag: Tag, tableName: String)
    extends FoxTable[A](tag, tableName) {

  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def onHand           = column[Int]("on_hand")
  def onHold           = column[Int]("on_hold")
  def reserved         = column[Int]("reserved")
  def availableForSale = column[Int]("available_for_sale")
  def updatedAt        = column[Instant]("updated_at")
}

abstract class InventorySummariesBase[A <: InventorySummaryBase[A],
    As <: InventorySummariesTableBase[A]](construct: Tag ⇒ As)
    extends FoxTableQuery[A, As](construct) {

  override type QuerySeq = Query[As, A, Seq]

  type Ret       = (Int, Int)
  type PackedRet = (Rep[Int], Rep[Int])
  override val returningQuery = map { o ⇒
    (o.id, o.availableForSale)
  }
}
