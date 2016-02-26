package models.inventory.adjustment

import java.time.Instant

import models.javaTimeSlickMapper
import monocle.macros.GenLens
import org.json4s.JValue
import slick.lifted.Tag
import utils.ExPostgresDriver.api._

final case class BackorderInventoryAdjustment(id: Int = 0, summaryId: Int, onHandChange: Int = 0, onHoldChange: Int = 0,
  reservedChange: Int = 0, metadata: JValue, createdAt: Instant = Instant.now)
  extends InventoryAdjustmentBase[BackorderInventoryAdjustment]

class BackorderInventoryAdjustments(tag: Tag)
  extends InventoryAdjustmentsTableBase[BackorderInventoryAdjustment](tag, "backorder_inventory_adjustments") {

  def * = (id, summaryId, onHandChange, onHoldChange, reservedChange, metadata, createdAt) <>(
    (BackorderInventoryAdjustment.apply _).tupled, BackorderInventoryAdjustment.unapply)
}

object BackorderInventoryAdjustments
  extends InventoryAdjustmentsBase[BackorderInventoryAdjustment, BackorderInventoryAdjustments](
    idLens = GenLens[BackorderInventoryAdjustment](_.id)
  )(new BackorderInventoryAdjustments(_))
