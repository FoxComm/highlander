package models

import java.time.Instant

import com.pellucid.sealerate
import models.Rma.{RmaType, Standard, Status, Pending}
import models.RmaLineItem.{OriginType, InventoryDisposition, Putaway}
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.{ADT, TableQueryWithId, GenericTable, ModelWithIdParameter}

final case class RmaLineItem(id: Int = 0, rmaId: Int, reasonId: Int, originId: Int, originType: OriginType,
  rmaType: RmaType = Standard, status: Status = Pending, inventoryDisposition: InventoryDisposition = Putaway,
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

  implicit val OriginTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] = OriginType.slickColumn
  implicit val InvDispColumnType: JdbcType[InventoryDisposition] with BaseTypedType[InventoryDisposition] =
    InventoryDisposition.slickColumn
}

class RmaLineItems(tag: Tag) extends GenericTable.TableWithId[RmaLineItem](tag, "rma_line_items")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def reasonId = column[Int]("reason_id")
  def originId = column[Int]("origin_id")
  def originType = column[OriginType]("origin_type")
  def rmaType = column[RmaType]("rma_type")
  def status = column[Status]("status")
  def inventoryDisposition = column[InventoryDisposition]("inventory_disposition")
  def createdAt = column[Instant]("created_at")

  def * = (id, rmaId, reasonId, originId, originType, rmaType, status,
    inventoryDisposition, createdAt) <> ((RmaLineItem.apply _).tupled, RmaLineItem.unapply)
}

object RmaLineItems extends TableQueryWithId[RmaLineItem, RmaLineItems](
  idLens = GenLens[RmaLineItem](_.id)
)(new RmaLineItems(_)) {
}
