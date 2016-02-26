package models.inventory.summary

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag

final case class SellableInventorySummary(id: Int = 0, onHand: Int, onHold: Int, reserved:
Int, safetyStock: Int) extends InventorySummaryBase[SellableInventorySummary] {

  override def availableForSale: Int = super.availableForSale - safetyStock
}

class SellableInventorySummaries(tag: Tag)
  extends InventorySummariesTableBase[SellableInventorySummary](tag, "sellable_inventory_summaries") {

  def safetyStock = column[Int]("safety_stock")

  def * = (id, onHand, onHold, reserved, safetyStock) <>((SellableInventorySummary.apply _).tupled,
    SellableInventorySummary.unapply)
}

object SellableInventorySummaries
  extends InventorySummariesBase[SellableInventorySummary, SellableInventorySummaries](
    idLens = GenLens[SellableInventorySummary](_.id)
  )(new SellableInventorySummaries(_))
