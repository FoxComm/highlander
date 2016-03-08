package models.inventory.adjustment

import java.time.Instant

import models.javaTimeSlickMapper
import monocle.macros.GenLens
import org.json4s.JValue
import slick.lifted.Tag
import utils.ExPostgresDriver.api._

final case class SellableInventoryAdjustment(id: Int = 0, summaryId: Int, onHandChange: Int = 0, onHoldChange: Int = 0,
  reservedChange: Int = 0, safetyStockChange: Int = 0, metadata: JValue, createdAt: Instant = Instant.now)
  extends InventoryAdjustmentBase[SellableInventoryAdjustment]

class SellableInventoryAdjustments(tag: Tag)
  extends InventoryAdjustmentsTableBase[SellableInventoryAdjustment](tag, "sellable_inventory_adjustments") {

  def safetyStockChange = column[Int]("safety_stock_change")

  def * = (id, summaryId, onHandChange, onHoldChange, reservedChange, safetyStockChange, metadata, createdAt) <>(
    (SellableInventoryAdjustment.apply _).tupled, SellableInventoryAdjustment.unapply)
}

object SellableInventoryAdjustments
  extends InventoryAdjustmentsBase[SellableInventoryAdjustment, SellableInventoryAdjustments](
    idLens = GenLens[SellableInventoryAdjustment](_.id)
  )(new SellableInventoryAdjustments(_))
