package models.inventory

import java.time.Instant

import cats.data.ValidatedNel
import cats.implicits._
import com.pellucid.sealerate
import models.inventory.InventoryAdjustment.State
import models.javaTimeSlickMapper
import monocle.macros.GenLens
import org.json4s.JsonAST.JValue
import services.Failure
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.lifted.Tag
import utils.ExPostgresDriver.api._
import utils.{ADT, GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

final case class InventoryAdjustment(id: Int = 0, summaryId: Int, change: Int, newQuantity: Int,
  newAfs: Int, state: State, skuType: SkuType, metadata: JValue, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[InventoryAdjustment] {

  import Validation._

  override def validate: ValidatedNel[Failure, InventoryAdjustment] =
    (validExpr(change != 0, "Changed quantity")
      |@| greaterThanOrEqual(newAfs, 0, "New AFS quantity")
      ).map { case _ ⇒ this }
}

object InventoryAdjustment {

  sealed trait AdjustmentEvent {
    def skuId: Int
    def warehouseId: Int
    val name: String
  }

  final case class WmsOverride(skuId: Int, warehouseId: Int, onHand: Int, onHold: Int, reserved: Int,
    name: String = "WMS Sync") extends AdjustmentEvent

  final case class OrderPlaced(skuId: Int, warehouseId: Int, orderRef: String, quantity: Int,
    name: String = "Order placed") extends AdjustmentEvent

  final case class OrderPropagated(skuId: Int, warehouseId: Int, orderRef: String, quantity: Int,
    name: String = "Order propagated") extends AdjustmentEvent

  sealed trait State
  case object OnHand extends State
  case object OnHold extends State
  case object Reserved extends State
  // TODO: safety stock

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn
}

class InventoryAdjustments(tag: Tag)
  extends GenericTable.TableWithId[InventoryAdjustment](tag, "inventory_adjustments") {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def summaryId = column[Int]("summary_id")
  def change = column[Int]("change")
  def newQuantity = column[Int]("new_quantity")
  def newAfs = column[Int]("new_afs")
  def state = column[State]("state")
  def skuType = column[SkuType]("sku_type")
  def metadata = column[JValue]("metadata")
  def createdAt = column[Instant]("created_at")

  def * = (id, summaryId, change, newQuantity, newAfs, state, skuType, metadata, createdAt) <>(
    (InventoryAdjustment.apply _).tupled, InventoryAdjustment.unapply)
}

object InventoryAdjustments extends TableQueryWithId[InventoryAdjustment, InventoryAdjustments](
  idLens = GenLens[InventoryAdjustment](_.id)
)(new InventoryAdjustments(_)) {

  def findSellableBySummaryId(summaryId: Int): QuerySeq =
    filter(_.summaryId === summaryId).filter(_.skuType === (Sellable: SkuType))
}
