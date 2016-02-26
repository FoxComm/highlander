package models.inventory.summary

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag

final case class NonSellableInventorySummary(id: Int = 0, onHand: Int, onHold: Int, reserved: Int)
  extends InventorySummaryBase[NonSellableInventorySummary]

class NonSellableInventorySummaries(tag: Tag)
  extends InventorySummariesTableBase[NonSellableInventorySummary](tag, "nonsellable_inventory_summaries") {

  def * = (id, onHand, onHold, reserved) <>((NonSellableInventorySummary.apply _).tupled,
    NonSellableInventorySummary.unapply)
}

object NonSellableInventorySummaries
  extends InventorySummariesBase[NonSellableInventorySummary, NonSellableInventorySummaries](
    idLens = GenLens[NonSellableInventorySummary](_.id)
  )(new NonSellableInventorySummaries(_))
