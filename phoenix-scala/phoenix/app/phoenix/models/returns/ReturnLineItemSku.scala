package phoenix.models.returns

import java.time.Instant

import core.db.ExPostgresDriver.api._
import core.db._
import objectframework.models._
import phoenix.models.inventory.Skus
import shapeless._

case class ReturnLineItemSku(id: Int,
                             returnId: Int,
                             quantity: Int,
                             skuId: Int,
                             skuShadowId: Int,
                             createdAt: Instant = Instant.now)
    extends FoxModel[ReturnLineItemSku]

class ReturnLineItemSkus(tag: Tag) extends FoxTable[ReturnLineItemSku](tag, "return_line_item_skus") {
  def id          = column[Int]("id", O.PrimaryKey)
  def returnId    = column[Int]("return_id")
  def quantity    = column[Int]("quantity")
  def skuId       = column[Int]("sku_id")
  def skuShadowId = column[Int]("sku_shadow_id")
  def createdAt   = column[Instant]("created_at")

  def * =
    (id, returnId, quantity, skuId, skuShadowId, createdAt) <> ((ReturnLineItemSku.apply _).tupled, ReturnLineItemSku.unapply)

  def li =
    foreignKey(ReturnLineItems.tableName, id, ReturnLineItems)(_.id, onDelete = ForeignKeyAction.Cascade)
  def sku    = foreignKey(Skus.tableName, skuId, Skus)(_.id)
  def shadow = foreignKey(ObjectShadows.tableName, skuShadowId, ObjectShadows)(_.id)
}

object ReturnLineItemSkus
    extends FoxTableQuery[ReturnLineItemSku, ReturnLineItemSkus](new ReturnLineItemSkus(_))
    with ReturningId[ReturnLineItemSku, ReturnLineItemSkus] {

  val returningLens: Lens[ReturnLineItemSku, Int] = lens[ReturnLineItemSku].id

  def findByRmaId(returnId: Rep[Int]): QuerySeq =
    filter(_.returnId === returnId)

  def findByContextAndCode(contextId: Int, code: String): QuerySeq =
    for {
      li  ← ReturnLineItemSkus
      sku ← li.sku if sku.code === code && sku.contextId === contextId
    } yield li
}
