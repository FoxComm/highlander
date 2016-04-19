package models.inventory.summary

import java.time.Instant
import models.javaTimeSlickMapper
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag

case class NonSellableInventorySummary(id: Int = 0, onHand: Int, onHold: Int, reserved: Int,
  val availableForSale: Int = 0, updatedAt: Instant = Instant.now)
  extends InventorySummaryBase[NonSellableInventorySummary]

class NonSellableInventorySummaries(tag: Tag)
  extends InventorySummariesTableBase[NonSellableInventorySummary](tag, "nonsellable_inventory_summaries") {

  def * = (id, onHand, onHold, reserved, availableForSale, updatedAt) <>((NonSellableInventorySummary.apply _).tupled,
    NonSellableInventorySummary.unapply)
}

object NonSellableInventorySummaries
  extends InventorySummariesBase[NonSellableInventorySummary, NonSellableInventorySummaries](
    idLens = GenLens[NonSellableInventorySummary](_.id)
  )(new NonSellableInventorySummaries(_)) {

  def returningAction(ret: (Int, Int))(summary: NonSellableInventorySummary): NonSellableInventorySummary = ret match {
    case (id, afs) ⇒ summary.copy(id = id, availableForSale = afs)
  }
}
