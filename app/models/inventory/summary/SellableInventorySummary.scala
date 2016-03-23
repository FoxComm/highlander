package models.inventory.summary

import java.time.Instant

import cats.data.ValidatedNel
import cats.implicits._
import failures.Failure
import models.javaTimeSlickMapper
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.Validation

final case class SellableInventorySummary(id: Int = 0, onHand: Int, onHold: Int, reserved: Int,
  availableForSale: Int = 0, safetyStock: Int, updatedAt: Instant = Instant.now)
  extends InventorySummaryBase[SellableInventorySummary] {

  import Validation._

  override def validate: ValidatedNel[Failure, SellableInventorySummary] =
    (greaterThanOrEqual(safetyStock, 0, "Safety stock quantity")
      |@| super.validate).map { case _ ⇒ this }
  }

class SellableInventorySummaries(tag: Tag)
  extends InventorySummariesTableBase[SellableInventorySummary](tag, "sellable_inventory_summaries") {

  def safetyStock = column[Int]("safety_stock")

  def * = (id, onHand, onHold, reserved, availableForSale, safetyStock, updatedAt) <>((SellableInventorySummary.apply _).tupled,
    SellableInventorySummary.unapply)
}

object SellableInventorySummaries
  extends InventorySummariesBase[SellableInventorySummary, SellableInventorySummaries](
    idLens = GenLens[SellableInventorySummary](_.id)
  )(new SellableInventorySummaries(_)) {

  def returningAction(ret: (Int, Int))(summary: SellableInventorySummary): SellableInventorySummary = ret match {
    case (id, afs) ⇒ summary.copy(id = id, availableForSale = afs)
  }
}
