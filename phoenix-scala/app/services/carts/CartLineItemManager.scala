package service.carts

import cats.data._
import cats.implicits._
import failures.ProductFailures.NoProductFoundForSku
import failures._
import models.cord.lineitems._
import models.inventory.Sku
import models.objects.{ProductSkuLinks, ProductVariantLinks, VariantValueLinks}
import models.product.VariantValueSkuLinks
import services.inventory.SkuManager
import services.product.ProductManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object CartLineItemManager {
  def getLineItems(
      refNum: String)(implicit ec: EC, db: DB, oc: OC): DbResultT[Seq[CartLineItemProductData]] =
    for {
      lineItems    ← * <~ CartLineItems.filter(_.cordRef === refNum).result
      lineItemData ← * <~ lineItems.map(getLineItemData)
    } yield lineItemData

  private def getLineItemData(lineItem: CartLineItem)(implicit ec: EC, db: DB, oc: OC) =
    for {
      sku ← * <~ SkuManager.mustFindFullSkuById(lineItem.skuId)
      productId ← * <~ ProductSkuLinks.filter(_.rightId === sku.model.id).one.dbresult.flatMap {
                   case Some(productLink) ⇒
                     DbResultT.good(productLink.leftId)
                   case None ⇒
                     for {
                       valueLink ← * <~ VariantValueSkuLinks
                                    .filter(_.rightId === sku.model.id)
                                    .mustFindOneOr(NoProductFoundForSku(sku.model.id, oc.id))
                       variantLink ← * <~ VariantValueLinks
                                      .filter(_.rightId === valueLink.leftId)
                                      .mustFindOneOr(NoProductFoundForSku(sku.model.id, oc.id))
                       productLink ← * <~ ProductVariantLinks
                                      .filter(_.rightId === variantLink.leftId)
                                      .mustFindOneOr(NoProductFoundForSku(sku.model.id, oc.id))
                     } yield productLink.leftId
                 }
      product ← * <~ ProductManager.mustFindFullProductById(productId)
    } yield
      CartLineItemProductData(sku = sku.model,
                              skuForm = sku.form,
                              skuShadow = sku.shadow,
                              product = product.shadow,
                              lineItem = lineItem)
}
