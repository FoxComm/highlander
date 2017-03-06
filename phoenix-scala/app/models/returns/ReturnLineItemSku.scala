package models.returns

import java.time.Instant
import models.inventory.{Sku, Skus}
import models.objects._
import shapeless._
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class ReturnLineItemSku(id: Int = 0,
                             returnId: Int,
                             skuId: Int,
                             skuShadowId: Int,
                             createdAt: Instant = Instant.now)
    extends FoxModel[ReturnLineItemSku]

object ReturnLineItemSku {}

class ReturnLineItemSkus(tag: Tag)
    extends FoxTable[ReturnLineItemSku](tag, "return_line_item_skus") {
  def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def returnId    = column[Int]("return_id")
  def skuId       = column[Int]("sku_id")
  def skuShadowId = column[Int]("sku_shadow_id")
  def createdAt   = column[Instant]("created_at")

  def * =
    (id, returnId, skuId, skuShadowId, createdAt) <> ((ReturnLineItemSku.apply _).tupled, ReturnLineItemSku.unapply)
  def sku    = foreignKey(Skus.tableName, skuId, Skus)(_.id)
  def shadow = foreignKey(ObjectShadows.tableName, skuShadowId, ObjectShadows)(_.id)
}

object ReturnLineItemSkus
    extends FoxTableQuery[ReturnLineItemSku, ReturnLineItemSkus](new ReturnLineItemSkus(_))
    with ReturningId[ReturnLineItemSku, ReturnLineItemSkus] {

  val returningLens: Lens[ReturnLineItemSku, Int] = lens[ReturnLineItemSku].id

  def findByRmaId(returnId: Rep[Int]): QuerySeq =
    filter(_.returnId === returnId)

  def findLineItemsByRma(rma: Return)(
      implicit ec: EC): DbResultT[Seq[(Sku, ObjectForm, ObjectShadow, ReturnLineItem)]] =
    (for {
      li     ← ReturnLineItems.filter(_.returnId === rma.id)
      liSku  ← li.skuLineItems
      sku    ← liSku.sku
      shadow ← liSku.shadow
      form   ← ObjectForms if form.id === sku.formId
    } yield (sku, form, shadow, li)).result.dbresult
}
