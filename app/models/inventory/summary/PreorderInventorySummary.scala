package models.inventory.summary

import java.time.Instant
import models.javaTimeSlickMapper
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag

final case class PreorderInventorySummary(id: Int = 0, onHand: Int, onHold: Int, reserved: Int,
  val availableForSale: Int = 0, updatedAt: Instant = Instant.now)
  extends InventorySummaryBase[PreorderInventorySummary]

class PreorderInventorySummaries(tag: Tag)
  extends InventorySummariesTableBase[PreorderInventorySummary](tag, "preorder_inventory_summaries") {

  def * = (id, onHand, onHold, reserved, availableForSale, updatedAt) <>((PreorderInventorySummary.apply _).tupled,
    PreorderInventorySummary.unapply)
}

object PreorderInventorySummaries
  extends InventorySummariesBase[PreorderInventorySummary, PreorderInventorySummaries](
    idLens = GenLens[PreorderInventorySummary](_.id)
  )(new PreorderInventorySummaries(_)) {

  def returningAction(ret: (Int, Int))(summary: PreorderInventorySummary): PreorderInventorySummary = ret match {
    case (id, afs) ⇒ summary.copy(id = id, availableForSale = afs)
  }
}
