package models

import java.time.Instant

import com.pellucid.sealerate
import models.Rma.Status
import models.RmaLineItem.{OriginType, InventoryDisposition, Putaway}
import monocle.macros.GenLens
import payloads.RmaSkuLineItemsPayload
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.{ADT, TableQueryWithId, GenericTable, ModelWithIdParameter}

final case class RmaLineItem(id: Int = 0, rmaId: Int, reasonId: Int, originId: Int, originType: OriginType,
  quantity: Int = 1, isReturnItem: Boolean = false, inventoryDisposition: InventoryDisposition = Putaway,
  createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[RmaLineItem] {

}

object RmaLineItem {
  sealed trait OriginType
  case object SkuItem extends OriginType
  case object GiftCardItem extends OriginType
  case object ShippingCost extends OriginType

  sealed trait InventoryDisposition
  case object Putaway extends InventoryDisposition
  case object Damage extends InventoryDisposition
  case object Recovery extends InventoryDisposition
  case object Discontinued extends InventoryDisposition

  object OriginType extends ADT[OriginType] {
    def types = sealerate.values[OriginType]
  }

  object InventoryDisposition extends ADT[InventoryDisposition] {
    def types = sealerate.values[InventoryDisposition]
  }

  def buildSku(rma: Rma, reason: RmaReason, origin: RmaLineItemSku, payload: RmaSkuLineItemsPayload): RmaLineItem = {
    RmaLineItem(
      rmaId = rma.id,
      reasonId = reason.id,
      quantity = payload.quantity,
      originId = origin.id,
      originType = SkuItem,
      isReturnItem = payload.isReturnItem,
      inventoryDisposition = payload.inventoryDisposition
    )
  }

  def buildGiftCard(rma: Rma, reason: RmaReason, origin: RmaLineItemGiftCard): RmaLineItem = {
    RmaLineItem(
      rmaId = rma.id,
      reasonId = reason.id,
      originId = origin.id,
      originType = GiftCardItem
    )
  }

  def buildShippinCost(rma: Rma, reason: RmaReason, origin: RmaLineItemShippingCost): RmaLineItem = {
    RmaLineItem(
      rmaId = rma.id,
      reasonId = reason.id,
      originId = origin.id,
      originType = ShippingCost
    )
  }

  implicit val OriginTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] = OriginType.slickColumn
  implicit val InvDispColumnType: JdbcType[InventoryDisposition] with BaseTypedType[InventoryDisposition] =
    InventoryDisposition.slickColumn
}

class RmaLineItems(tag: Tag) extends GenericTable.TableWithId[RmaLineItem](tag, "rma_line_items") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def reasonId = column[Int]("reason_id")
  def originId = column[Int]("origin_id")
  def originType = column[OriginType]("origin_type")
  def quantity = column[Int]("quantity")
  def isReturnItem = column[Boolean]("is_return_item")
  def inventoryDisposition = column[InventoryDisposition]("inventory_disposition")
  def createdAt = column[Instant]("created_at")

  def * = (id, rmaId, reasonId, originId, originType, quantity, isReturnItem,
    inventoryDisposition, createdAt) <> ((RmaLineItem.apply _).tupled, RmaLineItem.unapply)
}

object RmaLineItems extends TableQueryWithId[RmaLineItem, RmaLineItems](
  idLens = GenLens[RmaLineItem](_.id)
)(new RmaLineItems(_)) {
}
