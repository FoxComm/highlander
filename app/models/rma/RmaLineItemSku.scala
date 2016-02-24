package models.rma


import models.inventory.{Skus, Sku, SkuShadows, SkuShadow}
import models.javaTimeSlickMapper
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._

final case class RmaLineItemSku(id: Int = 0, rmaId: Int, skuId: Int, skuShadowId: Int, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[RmaLineItemSku]

object RmaLineItemSku {}

class RmaLineItemSkus(tag: Tag) extends GenericTable.TableWithId[RmaLineItemSku](tag, "rma_line_item_skus") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def skuId = column[Int]("sku_id")
  def skuShadowId = column[Int]("sku_shadow_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, rmaId, skuId, skuShadowId, createdAt) <> ((RmaLineItemSku.apply _).tupled, RmaLineItemSku.unapply)
  def sku = foreignKey(Skus.tableName, skuId, Skus)(_.id)
}

object RmaLineItemSkus extends TableQueryWithId[RmaLineItemSku, RmaLineItemSkus](
  idLens = GenLens[RmaLineItemSku](_.id)
)(new RmaLineItemSkus(_)){

  def findByRmaId(rmaId: Rep[Int]): QuerySeq =
    filter(_.rmaId === rmaId)

  def findLineItemsByRma(rma: Rma): Query[(Skus, SkuShadows, RmaLineItems), (Sku, SkuShadow, RmaLineItem), Seq] = for {
    li ← RmaLineItems.filter(_.rmaId === rma.id)
    liSku ← li.skuLineItems
    skuShadow ← SkuShadows if skuShadow.id === liSku.skuShadowId
    sku ← skuShadow.sku
  } yield (sku, skuShadow, li)

}
