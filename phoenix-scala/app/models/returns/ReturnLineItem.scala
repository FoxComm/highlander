package models.returns

import com.pellucid.sealerate
import java.time.Instant
import models.returns.ReturnLineItem._
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.ADT
import utils.db._

case class ReturnLineItem(id: Int = 0,
                          returnId: Int,
                          reasonId: Int,
                          originType: OriginType,
                          quantity: Int = 1,
                          inventoryDisposition: InventoryDisposition = Putaway,
                          createdAt: Instant = Instant.now)
    extends FoxModel[ReturnLineItem]

object ReturnLineItem {
  sealed trait OriginType extends Product with Serializable
  case object SkuItem      extends OriginType
  case object ShippingCost extends OriginType

  implicit object OriginType extends ADT[OriginType] {
    def types = sealerate.values[OriginType]
  }

  sealed trait InventoryDisposition
  case object Putaway      extends InventoryDisposition
  case object Damage       extends InventoryDisposition
  case object Recovery     extends InventoryDisposition
  case object Discontinued extends InventoryDisposition

  object InventoryDisposition extends ADT[InventoryDisposition] {
    def types = sealerate.values[InventoryDisposition]
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
  def originType           = column[OriginType]("origin_type")
  def quantity             = column[Int]("quantity")
  def inventoryDisposition = column[InventoryDisposition]("inventory_disposition")
  def createdAt            = column[Instant]("created_at")

  def * =
    (id, returnId, reasonId, originType, quantity, inventoryDisposition, createdAt) <> ((ReturnLineItem.apply _).tupled, ReturnLineItem.unapply)
}

object ReturnLineItems
    extends FoxTableQuery[ReturnLineItem, ReturnLineItems](new ReturnLineItems(_))
    with ReturningId[ReturnLineItem, ReturnLineItems] {
  val returningLens: Lens[ReturnLineItem, Int] = lens[ReturnLineItem].id

  def findByRmaId(returnId: Int): QuerySeq =
    filter(_.returnId === returnId)
}
