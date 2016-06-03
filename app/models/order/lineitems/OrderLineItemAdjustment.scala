package models.order.lineitems

import com.pellucid.sealerate
import shapeless._
import utils.ADT
import utils.db._
import utils.db.ExPostgresDriver.api._
import slick.jdbc.JdbcType
import java.time.Instant

import slick.ast.BaseTypedType

final case class OrderLineItemAdjustment(id: Int = 0,
                                         orderId: Int,
                                         promotionShadowId: Int,
                                         adjustmentType: OrderLineItemAdjustment.AdjustmentType,
                                         substract: Int,
                                         lineItemRefNum: Option[String] = None,
                                         createdAt: Instant = Instant.now)
    extends FoxModel[OrderLineItemAdjustment]

class OrderLineItemAdjustments(tag: Tag)
    extends FoxTable[OrderLineItemAdjustment](tag, "order_line_item_adjustments") {

  def id                = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId           = column[Int]("order_id")
  def promotionShadowId = column[Int]("promotion_shadow_id")
  def adjustmentType    = column[OrderLineItemAdjustment.AdjustmentType]("adjustment_type")
  def substract         = column[Int]("substract")
  def lineItemRefNum    = column[Option[String]]("line_item_ref_num")
  def createdAt         = column[Instant]("created_at")

  def * =
    (id, orderId, promotionShadowId, adjustmentType, substract, lineItemRefNum, createdAt) <> ((OrderLineItemAdjustment.apply _).tupled, OrderLineItemAdjustment.unapply)
}

object OrderLineItemAdjustment {
  sealed trait AdjustmentType
  case object LineItemAdjustment extends AdjustmentType
  case object OrderAdjustment    extends AdjustmentType
  case object ShippingAdjustment extends AdjustmentType
  case object Combinator         extends AdjustmentType

  object AdjustmentType extends ADT[AdjustmentType] {
    def types = sealerate.values[AdjustmentType]
  }

  implicit val adjustmentTypeColumnType: JdbcType[AdjustmentType] with BaseTypedType[
      AdjustmentType] = AdjustmentType.slickColumn
}

object OrderLineItemAdjustments
    extends FoxTableQuery[OrderLineItemAdjustment, OrderLineItemAdjustments](
        new OrderLineItemAdjustments(_))
    with ReturningId[OrderLineItemAdjustment, OrderLineItemAdjustments] {

  val returningLens: Lens[OrderLineItemAdjustment, Int] = lens[OrderLineItemAdjustment].id

  def findByOrderId(orderId: Int): QuerySeq =
    filter(_.orderId === orderId)

  def filterByOrderIdAndShadow(orderId: Int, shadowId: Int): QuerySeq =
    filter(_.orderId === orderId).filter(_.promotionShadowId === shadowId)

  def filterByOrderIdAndShadows(orderId: Int, shadows: Seq[Int]): QuerySeq =
    filter(_.orderId === orderId).filter(_.promotionShadowId.inSet(shadows))
}
