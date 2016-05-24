package models.inventory.summary

import java.time.Instant
import shapeless._
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.db.javaTimeSlickMapper

case class NonSellableInventorySummary(id: Int = 0,
                                       onHand: Int,
                                       onHold: Int,
                                       reserved: Int,
                                       availableForSale: Int = 0,
                                       updatedAt: Instant = Instant.now)
    extends InventorySummaryBase[NonSellableInventorySummary]

class NonSellableInventorySummaries(tag: Tag)
    extends InventorySummariesTableBase[NonSellableInventorySummary](
        tag, "nonsellable_inventory_summaries") {

  def * =
    (id, onHand, onHold, reserved, availableForSale, updatedAt) <> ((NonSellableInventorySummary.apply _).tupled,
        NonSellableInventorySummary.unapply)
}

object NonSellableInventorySummaries
    extends InventorySummariesBase[NonSellableInventorySummary, NonSellableInventorySummaries](
        new NonSellableInventorySummaries(_)) {

  private val rootLens = lens[NonSellableInventorySummary]
  val returningLens: Lens[NonSellableInventorySummary, (Int, Int)] =
    rootLens.id ~ rootLens.availableForSale
}
