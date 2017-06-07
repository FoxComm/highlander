package models.inventory

import java.time.Instant

import shapeless._
import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.NotFoundFailure400
import phoenix.models.inventory.Skus

/**
  * This entity is a relation of SKU in Phoenix to SKU stored in Middlewarehouse.
  * Upon SKU creation in MWH, a unique SKU id returned by MWH must be associated with a SKU that was
  * created for.
  */
case class Sku2MwhSku(id: Int = 0, skuId: Int, mwhSkuId: Int, createdAt: Instant = Instant.now)
    extends FoxModel[Sku2MwhSku]

class Sku2MwhSkus(tag: Tag) extends FoxTable[Sku2MwhSku](tag, "sku_mwh_sku_ids") {

  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId     = column[Int]("sku_id")
  def mwhSkuId  = column[Int]("mwh_sku_id")
  def createdAt = column[Instant]("created_at")

  def * =
    (id, skuId, mwhSkuId, createdAt) <> ((Sku2MwhSku.apply _).tupled, Sku2MwhSku.unapply)

  def sku =
    foreignKey(Skus.tableName, skuId, Skus)(_.formId)
}

object Sku2MwhSkus
    extends FoxTableQuery[Sku2MwhSku, Sku2MwhSkus](new Sku2MwhSkus(_))
    with ReturningId[Sku2MwhSku, Sku2MwhSkus] {

  val returningLens: Lens[Sku2MwhSku, Int] = lens[Sku2MwhSku].id

  def bySkuId(skuId: Int): QuerySeq =
    filter(_.skuId === skuId)

  def mustFindBySkuId(skuId: Int)(implicit ec: EC): DbResultT[Sku2MwhSku] =
    bySkuId(skuId).mustFindOneOr(NotFoundFailure400(s"SKU not found for variant with id=$skuId"))

  def mustFindMwhSkuId(skuId: Int)(implicit ec: EC): DbResultT[Int] =
    bySkuId(skuId)
      .map(_.mwhSkuId)
      .mustFindOneOr(NotFoundFailure400(s"Middlewarehouse SKU id not found for variant with id=$skuId"))
}
