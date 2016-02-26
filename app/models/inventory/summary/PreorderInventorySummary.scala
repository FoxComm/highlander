package models.inventory.summary

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag

final case class PreorderInventorySummary(id: Int = 0, onHand: Int, onHold: Int, reserved:
Int) extends InventorySummaryBase[PreorderInventorySummary]

class PreorderInventorySummaries(tag: Tag)
  extends InventorySummariesTableBase[PreorderInventorySummary](tag, "preorder_inventory_summaries") {

  def * = (id, onHand, onHold, reserved) <>((PreorderInventorySummary.apply _).tupled, PreorderInventorySummary.unapply)
}

object PreorderInventorySummaries
  extends InventorySummariesBase[PreorderInventorySummary, PreorderInventorySummaries](
    idLens = GenLens[PreorderInventorySummary](_.id)
  )(new PreorderInventorySummaries(_))
