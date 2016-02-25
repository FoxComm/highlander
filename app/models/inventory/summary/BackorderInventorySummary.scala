package models.inventory.summary

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag

final case class BackorderInventorySummary(id: Int = 0, onHand: Int, onHold: Int, reserved: Int)
  extends InventorySummaryBase[BackorderInventorySummary]

class BackorderInventorySummaries(tag: Tag)
extends InventorySummariesTableBase[BackorderInventorySummary](tag, "backorder_inventory_summaries") {

  def * = (id, onHand, onHold, reserved) <>((BackorderInventorySummary.apply _).tupled,
    BackorderInventorySummary.unapply)
}

object BackorderInventorySummaries
  extends InventorySummariesBase[BackorderInventorySummary, BackorderInventorySummaries](
    idLens = GenLens[BackorderInventorySummary](_.id)
  )(new BackorderInventorySummaries(_))
