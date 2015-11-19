package models

import java.time.Instant

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class RmaLineItemSku(id: Int = 0, rmaId: Int, skuId: Int, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[RmaLineItemSku]

object RmaLineItemSku {}

class RmaLineItemSkus(tag: Tag) extends GenericTable.TableWithId[RmaLineItemSku](tag, "rma_line_item_skus") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def skuId = column[Int]("sku_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, rmaId, skuId, createdAt) <> ((RmaLineItemSku.apply _).tupled, RmaLineItemSku.unapply)
  def sku = foreignKey(Skus.tableName, skuId, Skus)(_.id)
}

object RmaLineItemSkus extends TableQueryWithId[RmaLineItemSku, RmaLineItemSkus](
  idLens = GenLens[RmaLineItemSku](_.id)
)(new RmaLineItemSkus(_)){

  def findByRmaId(rmaId: Rep[Int]): QuerySeq =
    filter(_.rmaId === rmaId)

  def findLineItemsByRma(rma: Rma): Query[(Skus, RmaLineItems), (Sku, RmaLineItem), Seq] = for {
    liSku ← findByRmaId(rma.id)
    li ← RmaLineItems if li.originId === liSku.id
    sku ← Skus if sku.id === liSku.skuId
  } yield (sku, li)

}
