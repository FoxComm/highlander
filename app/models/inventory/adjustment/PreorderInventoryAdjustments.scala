package models.inventory.adjustment

import java.time.Instant

import models.javaTimeSlickMapper
import monocle.macros.GenLens
import org.json4s.JValue
import slick.lifted.Tag
import utils.ExPostgresDriver.api._

final case class PreorderInventoryAdjustment(id: Int = 0, summaryId: Int, onHandChange: Int = 0, onHoldChange: Int = 0,
  reservedChange: Int = 0, metadata: JValue, createdAt: Instant = Instant.now)
  extends InventoryAdjustmentBase[PreorderInventoryAdjustment]

class PreorderInventoryAdjustments(tag: Tag)
  extends InventoryAdjustmentsTableBase[PreorderInventoryAdjustment](tag, "preorder_inventory_adjustments") {

  def * = (id, summaryId, onHandChange, onHoldChange, reservedChange, metadata, createdAt) <>(
    (PreorderInventoryAdjustment.apply _).tupled, PreorderInventoryAdjustment.unapply)
}

object PreorderInventoryAdjustments
  extends InventoryAdjustmentsBase[PreorderInventoryAdjustment, PreorderInventoryAdjustments](
    idLens = GenLens[PreorderInventoryAdjustment](_.id)
  )(new PreorderInventoryAdjustments(_))
