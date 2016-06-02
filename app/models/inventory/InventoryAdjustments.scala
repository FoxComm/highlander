package models.inventory

import java.time.Instant

import cats.data.ValidatedNel
import cats.implicits._
import com.pellucid.sealerate
import failures.Failure
import models.inventory.InventoryAdjustment._
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.lifted.Tag
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.{ADT, Validation}

case class InventoryAdjustment(id: Int = 0,
                               summaryId: Int,
                               change: Int,
                               newQuantity: Int,
                               newAfs: Int,
                               state: State,
                               skuType: SkuType,
                               metadata: Json,
                               createdAt: Instant = Instant.now)
    extends FoxModel[InventoryAdjustment] {

  import Validation._

  override def validate: ValidatedNel[Failure, InventoryAdjustment] =
    (validExpr(change != 0, "Changed quantity") |@| greaterThanOrEqual(
            newAfs, 0, "New AFS quantity") |@| validExpr(
            safetyStockSellableOnly, "Only sellable SKUs can have safety stock adjustments")).map {
      case _ ⇒ this
    }

  private def safetyStockSellableOnly = state match {
    case SafetyStock ⇒ skuType == Sellable
    case _           ⇒ true
  }
}

object InventoryAdjustment {

  sealed trait AdjustmentEvent {
    def skuId: Int
    def warehouseId: Int
    val name: String
  }

  case class WmsOverride(skuId: Int,
                         warehouseId: Int,
                         onHand: Int,
                         onHold: Int,
                         reserved: Int,
                         name: String = "WMS Sync")
      extends AdjustmentEvent

  case class OrderPlaced(
      skuId: Int, warehouseId: Int, orderRef: String, quantity: Int, name: String = "Order placed")
      extends AdjustmentEvent

  case class OrderPropagated(skuId: Int,
                             warehouseId: Int,
                             orderRef: String,
                             quantity: Int,
                             name: String = "Order propagated")
      extends AdjustmentEvent

  sealed trait State
  case object OnHand   extends State
  case object OnHold   extends State
  case object Reserved extends State

  case object SafetyStock extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn
}

class InventoryAdjustments(tag: Tag)
    extends FoxTable[InventoryAdjustment](tag, "inventory_adjustments") {

  def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def summaryId   = column[Int]("summary_id")
  def change      = column[Int]("change")
  def newQuantity = column[Int]("new_quantity")
  def newAfs      = column[Int]("new_afs")
  def state       = column[State]("state")
  def skuType     = column[SkuType]("sku_type")
  def metadata    = column[Json]("metadata")
  def createdAt   = column[Instant]("created_at")

  def * =
    (id, summaryId, change, newQuantity, newAfs, state, skuType, metadata, createdAt) <> ((InventoryAdjustment.apply _).tupled, InventoryAdjustment.unapply)
}

object InventoryAdjustments
    extends FoxTableQuery[InventoryAdjustment, InventoryAdjustments](new InventoryAdjustments(_))
    with ReturningId[InventoryAdjustment, InventoryAdjustments] {

  val returningLens: Lens[InventoryAdjustment, Int] = lens[InventoryAdjustment].id

  def findSellableBySummaryId(summaryId: Int): QuerySeq =
    filter(_.summaryId === summaryId).filter(_.skuType === (Sellable: SkuType))
}
