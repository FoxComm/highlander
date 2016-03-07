package models.inventory.adjustment

import java.time.Instant

import models.javaTimeSlickMapper
import monocle.macros.GenLens
import org.json4s.JValue
import slick.lifted.Tag
import utils.ExPostgresDriver.api._

final case class NonSellableInventoryAdjustment(id: Int = 0, summaryId: Int, onHandChange: Int = 0, onHoldChange: Int = 0,
  reservedChange: Int = 0, metadata: JValue, createdAt: Instant = Instant.now)
  extends InventoryAdjustmentBase[NonSellableInventoryAdjustment]

class NonSellableInventoryAdjustments(tag: Tag)
  extends InventoryAdjustmentsTableBase[NonSellableInventoryAdjustment](tag, "nonsellable_inventory_adjustments") {

  def * = (id, summaryId, onHandChange, onHoldChange, reservedChange, metadata, createdAt) <>(
    (NonSellableInventoryAdjustment.apply _).tupled, NonSellableInventoryAdjustment.unapply)
}

object NonSellableInventoryAdjustments
  extends InventoryAdjustmentsBase[NonSellableInventoryAdjustment, NonSellableInventoryAdjustments](
    idLens = GenLens[NonSellableInventoryAdjustment](_.id)
  )(new NonSellableInventoryAdjustments(_))
