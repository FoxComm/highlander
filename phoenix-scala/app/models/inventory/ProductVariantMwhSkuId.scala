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
case class ProductVariantMwhSkuId(id: Int = 0,
                                  variantFormId: Int,
                                  mwhSkuId: Int,
                                  createdAt: Instant = Instant.now)
    extends FoxModel[ProductVariantMwhSkuId]

class ProductVariantMwhSkuIds(tag: Tag)
    extends FoxTable[ProductVariantMwhSkuId](tag, "product_variant_mwh_sku_ids") {

  def id            = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def variantFormId = column[Int]("variant_form_id")
  def mwhSkuId      = column[Int]("mwh_sku_id")
  def createdAt     = column[Instant]("created_at")

  def * =
    (id, variantFormId, mwhSkuId, createdAt) <> ((ProductVariantMwhSkuId.apply _).tupled, ProductVariantMwhSkuId.unapply)

  def productVariant =
    foreignKey(ProductVariants.tableName, variantFormId, ProductVariants)(_.formId)
}

object ProductVariantMwhSkuIds
    extends FoxTableQuery[ProductVariantMwhSkuId, ProductVariantMwhSkuIds](
        new ProductVariantMwhSkuIds(_))
    with ReturningId[ProductVariantMwhSkuId, ProductVariantMwhSkuIds] {

  val returningLens: Lens[ProductVariantMwhSkuId, Int] = lens[ProductVariantMwhSkuId].id

  def byVariantFormId(variantFormId: Int): QuerySeq =
    filter(_.variantFormId === variantFormId)

  def mustFindMwhSkuId(variantFormId: Int)(implicit ec: EC): DbResultT[Int] =
    byVariantFormId(variantFormId)
      .map(_.mwhSkuId)
      .mustFindOneOr(NotFoundFailure400(
              s"Middlwarehouse SKU id not found for variant with id=$variantFormId"))
}
