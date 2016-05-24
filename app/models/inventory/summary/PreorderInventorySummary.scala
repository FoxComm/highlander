package models.inventory.summary

import java.time.Instant
import shapeless._
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.db.javaTimeSlickMapper

case class PreorderInventorySummary(id: Int = 0,
                                    onHand: Int,
                                    onHold: Int,
                                    reserved: Int,
                                    availableForSale: Int = 0,
                                    updatedAt: Instant = Instant.now)
    extends InventorySummaryBase[PreorderInventorySummary]

class PreorderInventorySummaries(tag: Tag)
    extends InventorySummariesTableBase[PreorderInventorySummary](
        tag, "preorder_inventory_summaries") {

  def * =
    (id, onHand, onHold, reserved, availableForSale, updatedAt) <> ((PreorderInventorySummary.apply _).tupled,
        PreorderInventorySummary.unapply)
}

object PreorderInventorySummaries
    extends InventorySummariesBase[PreorderInventorySummary, PreorderInventorySummaries](
        new PreorderInventorySummaries(_)) {

  private val rootLens = lens[PreorderInventorySummary]
  val returningLens: Lens[PreorderInventorySummary, (Int, Int)] =
    rootLens.id ~ rootLens.availableForSale
}
