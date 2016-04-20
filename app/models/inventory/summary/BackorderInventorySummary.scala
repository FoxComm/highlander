package models.inventory.summary

import java.time.Instant
import monocle.macros.GenLens
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
  extends InventorySummariesBase[BackorderInventorySummary, BackorderInventorySummaries](
    idLens = GenLens[BackorderInventorySummary](_.id)
  )(new BackorderInventorySummaries(_)) {

  def returningAction(ret: (Int, Int))(summary: BackorderInventorySummary): BackorderInventorySummary = ret match {
    case (id, afs) ⇒ summary.copy(id = id, availableForSale = afs)
  }
}
