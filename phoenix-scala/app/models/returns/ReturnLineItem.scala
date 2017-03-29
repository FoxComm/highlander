package models.returns

import java.time.Instant

import com.pellucid.sealerate
import models.returns.ReturnLineItem._
import shapeless._
import payloads.ReturnPayloads.ReturnSkuLineItemPayload
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.db._
import utils.ADT

case class ReturnLineItem(id: Int = 0,
                          referenceNumber: String = "",
                          returnId: Int,
                          reasonId: Int,
                          originId: Int,
                          originType: OriginType,
                          quantity: Int = 1,
                          isReturnItem: Boolean = false,
                          inventoryDisposition: InventoryDisposition = Putaway,
                          createdAt: Instant = Instant.now)
    extends FoxModel[ReturnLineItem] {}

object ReturnLineItem {
  sealed trait OriginType extends Product with Serializable
  case object SkuItem      extends OriginType
  case object GiftCardItem extends OriginType
  case object ShippingCost extends OriginType

  sealed trait InventoryDisposition
  case object Putaway      extends InventoryDisposition
  case object Damage       extends InventoryDisposition
  case object Recovery     extends InventoryDisposition
  case object Discontinued extends InventoryDisposition

  implicit object OriginType extends ADT[OriginType] {
    def types = sealerate.values[OriginType]
  }

  object InventoryDisposition extends ADT[InventoryDisposition] {
    def types = sealerate.values[InventoryDisposition]
  }

  def buildSku(rma: Return,
               reason: ReturnReason,
               origin: ReturnLineItemSku,
               payload: ReturnSkuLineItemPayload): ReturnLineItem = {
    ReturnLineItem(
        returnId = rma.id,
        reasonId = reason.id,
        quantity = payload.quantity,
        originId = origin.id,
        originType = SkuItem,
        isReturnItem = payload.isReturnItem,
        inventoryDisposition = payload.inventoryDisposition
    )
  }

  def buildGiftCard(rma: Return,
                    reason: ReturnReason,
                    origin: ReturnLineItemGiftCard): ReturnLineItem = {
    ReturnLineItem(
        returnId = rma.id,
        reasonId = reason.id,
        originId = origin.id,
        originType = GiftCardItem
    )
  }

  def buildShippingCost(rma: Return,
                        reason: ReturnReason,
                        origin: ReturnLineItemShippingCost): ReturnLineItem = {
    ReturnLineItem(
        returnId = rma.id,
        reasonId = reason.id,
        originId = origin.id,
        originType = ShippingCost
    )
  }

  implicit val OriginTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] =
    OriginType.slickColumn
  implicit val InvDispColumnType: JdbcType[InventoryDisposition] with BaseTypedType[
      InventoryDisposition] = InventoryDisposition.slickColumn
}

class ReturnLineItems(tag: Tag) extends FoxTable[ReturnLineItem](tag, "return_line_items") {
  def id                   = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber      = column[String]("reference_number")
  def returnId             = column[Int]("return_id")
  def reasonId             = column[Int]("reason_id")
  def originId             = column[Int]("origin_id")
  def originType           = column[OriginType]("origin_type")
  def quantity             = column[Int]("quantity")
  def isReturnItem         = column[Boolean]("is_return_item")
  def inventoryDisposition = column[InventoryDisposition]("inventory_disposition")
  def createdAt            = column[Instant]("created_at")

  def * =
    (id,
     referenceNumber,
     returnId,
     reasonId,
     originId,
     originType,
     quantity,
     isReturnItem,
     inventoryDisposition,
     createdAt) <> ((ReturnLineItem.apply _).tupled, ReturnLineItem.unapply)

  def skuLineItems = foreignKey(ReturnLineItemSkus.tableName, originId, ReturnLineItemSkus)(_.id)
  def shippingCostLineItems =
    foreignKey(ReturnLineItemShippingCosts.tableName, originId, ReturnLineItemShippingCosts)(_.id)
}

object ReturnLineItems
    extends FoxTableQuery[ReturnLineItem, ReturnLineItems](new ReturnLineItems(_))
    with ReturningId[ReturnLineItem, ReturnLineItems] {
  val returningLens: Lens[ReturnLineItem, Int] = lens[ReturnLineItem].id
}
