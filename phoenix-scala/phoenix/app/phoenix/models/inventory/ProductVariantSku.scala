package phoenix.models.inventory

import java.time.Instant

import shapeless._
import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.NotFoundFailure400

/**
  * This entity is a relation of ProductVariant (currently called SKU in Phoenix but this is temporary)
  * to SKU stored in Middlewarehouse. Upon SKU creation in MWH, a unique SKU id returned by MWH must be
  * associated with a SKU that was created for.
  */
case class ProductVariantSku(id: Int = 0, skuId: Int, mwhSkuId: Int, createdAt: Instant = Instant.now)
    extends FoxModel[ProductVariantSku]

class ProductVariantSkus(tag: Tag) extends FoxTable[ProductVariantSku](tag, "product_variant_sku_ids") {

  def id            = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def variantFormId = column[Int]("variant_form_id")
  def mwhSkuId      = column[Int]("mwh_sku_id")
  def createdAt     = column[Instant]("created_at")

  def * =
    (id, variantFormId, mwhSkuId, createdAt) <> ((ProductVariantSku.apply _).tupled, ProductVariantSku.unapply)

  def sku =
    foreignKey(Skus.tableName, variantFormId, Skus)(_.formId)
}

object ProductVariantSkus
    extends FoxTableQuery[ProductVariantSku, ProductVariantSkus](new ProductVariantSkus(_))
    with ReturningId[ProductVariantSku, ProductVariantSkus] {

  val returningLens: Lens[ProductVariantSku, Int] = lens[ProductVariantSku].id
}
