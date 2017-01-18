package models.cord.lineitems

import java.time.Instant

import com.pellucid.sealerate
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.ADT
import utils.db.ExPostgresDriver.api._
import utils.db._

final case class OrderLineItemAdjustment(id: Int = 0,
                                         cordRef: String,
                                         promotionShadowId: Int,
                                         adjustmentType: OrderLineItemAdjustment.AdjustmentType,
                                         subtract: Int,
                                         lineItemRefNum: Option[String] = None,
                                         createdAt: Instant = Instant.now)
    extends FoxModel[OrderLineItemAdjustment]

class OrderLineItemAdjustments(tag: Tag)
    extends FoxTable[OrderLineItemAdjustment](tag, "order_line_item_adjustments") {

  def id                = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cordRef           = column[String]("cord_ref")
  def promotionShadowId = column[Int]("promotion_shadow_id")
  def adjustmentType    = column[OrderLineItemAdjustment.AdjustmentType]("adjustment_type")
  def subtract          = column[Int]("subtract")
  def lineItemRefNum    = column[Option[String]]("line_item_ref_num")
  def createdAt         = column[Instant]("created_at")

  def * =
    (id, cordRef, promotionShadowId, adjustmentType, subtract, lineItemRefNum, createdAt) <> ((OrderLineItemAdjustment.apply _).tupled, OrderLineItemAdjustment.unapply)
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

  def findByCordRef(cordRef: String): QuerySeq =
    filter(_.cordRef === cordRef)

  def filterByOrderRefAndShadow(cordRef: String, shadowId: Int): QuerySeq =
    filter(_.cordRef === cordRef).filter(_.promotionShadowId === shadowId)

  def filterByOrderRefAndShadows(cordRef: String, shadows: Seq[Int]): QuerySeq =
    filter(_.cordRef === cordRef).filter(_.promotionShadowId.inSet(shadows))
}
