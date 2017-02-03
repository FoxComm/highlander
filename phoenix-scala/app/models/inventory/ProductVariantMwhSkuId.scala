package models.inventory

import java.time.Instant

import failures.NotFoundFailure400
import shapeless._
import utils.aliases.EC
import utils.db.ExPostgresDriver.api._
import utils.db._

/**
  * This entity is a relation of product variant to SKU stored in Middlewarehouse.
  * Upon SKU creation in MWH, a unique SKU id returned my MWH must be associated with a product variant SKU was
  * created for.
  */
case class ProductVariantSku(id: Int = 0,
                             variantFormId: Int,
                             skuId: Int,
                             skuCode: String,
                             createdAt: Instant = Instant.now)
    extends FoxModel[ProductVariantSku]

class ProductVariantSkus(tag: Tag)
    extends FoxTable[ProductVariantSku](tag, "product_variant_skus") {

  def id            = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def variantFormId = column[Int]("variant_form_id")
  def skuId         = column[Int]("sku_id")
  def skuCode       = column[String]("sku_code")
  def createdAt     = column[Instant]("created_at")

  def * =
    (id, variantFormId, skuId, skuCode, createdAt) <> ((ProductVariantSku.apply _).tupled, ProductVariantSku.unapply)

  def productVariant =
    foreignKey(ProductVariants.tableName, variantFormId, ProductVariants)(_.formId)
}

object ProductVariantSkus
    extends FoxTableQuery[ProductVariantSku, ProductVariantSkus](new ProductVariantSkus(_))
    with ReturningId[ProductVariantSku, ProductVariantSkus] {

  val returningLens: Lens[ProductVariantSku, Int] = lens[ProductVariantSku].id

  def byVariantFormId(variantFormId: Int): QuerySeq =
    filter(_.variantFormId === variantFormId)

  def mustFindSkuId(variantFormId: Int)(implicit ec: EC): DbResultT[Int] =
    byVariantFormId(variantFormId)
      .map(_.skuId)
      .mustFindOneOr(NotFoundFailure400(
              s"Middlewarehouse SKU id not found for variant with id=$variantFormId"))
}
