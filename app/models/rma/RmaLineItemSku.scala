package models.rma

import java.time.Instant

import models.inventory.{Sku, Skus}
import models.objects._
import monocle.macros.GenLens
import utils.db.ExPostgresDriver.api._
import utils.db._

case class RmaLineItemSku(id: Int = 0, rmaId: Int, skuId: Int, skuShadowId: Int, createdAt: Instant = Instant.now)
  extends FoxModel[RmaLineItemSku]

object RmaLineItemSku {}

class RmaLineItemSkus(tag: Tag) extends FoxTable[RmaLineItemSku](tag, "rma_line_item_skus") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def skuId = column[Int]("sku_id")
  def skuShadowId = column[Int]("sku_shadow_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, rmaId, skuId, skuShadowId, createdAt) <> ((RmaLineItemSku.apply _).tupled, RmaLineItemSku.unapply)
  def sku = foreignKey(Skus.tableName, skuId, Skus)(_.id)
  def shadow = foreignKey(ObjectShadows.tableName, skuShadowId, ObjectShadows)(_.id)
}

object RmaLineItemSkus extends FoxTableQuery[RmaLineItemSku, RmaLineItemSkus](
  idLens = GenLens[RmaLineItemSku](_.id)
)(new RmaLineItemSkus(_)){

  def findByRmaId(rmaId: Rep[Int]): QuerySeq =
    filter(_.rmaId === rmaId)

  def findLineItemsByRma(rma: Rma): Query[(Skus, ObjectForms, ObjectShadows, RmaLineItems), 
  (Sku, ObjectForm, ObjectShadow, RmaLineItem), Seq] = for {
    li ← RmaLineItems.filter(_.rmaId === rma.id)
    liSku ← li.skuLineItems
    sku ← liSku.sku
    shadow ← liSku.shadow
    form ← ObjectForms if form.id === sku.formId
  } yield (sku, form, shadow, li)

}
