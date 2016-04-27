package models.inventory.summary

import java.time.Instant
import shapeless._
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.db.javaTimeSlickMapper

case class BackorderInventorySummary(id: Int = 0, onHand: Int, onHold: Int, reserved: Int,
  availableForSale: Int = 0, updatedAt: Instant = Instant.now)
  extends InventorySummaryBase[BackorderInventorySummary]

class BackorderInventorySummaries(tag: Tag)
extends InventorySummariesTableBase[BackorderInventorySummary](tag, "backorder_inventory_summaries") {

  def * = (id, onHand, onHold, reserved, availableForSale, updatedAt) <>((BackorderInventorySummary.apply _).tupled,
    BackorderInventorySummary.unapply)
}

object BackorderInventorySummaries
  extends InventorySummariesBase[BackorderInventorySummary, BackorderInventorySummaries](new BackorderInventorySummaries(_)) {

  private val rootLens = lens[BackorderInventorySummary]
  val returningLens: Lens[BackorderInventorySummary, (Int, Int)] = rootLens.id ~ rootLens.availableForSale
}
