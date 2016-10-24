package services

import cats.data._
import cats.implicits._
import failures.ProductFailures.NoProductFoundForSku
import models.cord.lineitems._
import models.inventory.Sku
import models.objects._
import models.product._
import services.inventory.SkuManager
import services.product.ProductManager
import slick.driver.PostgresDriver.api._
import utils._
import utils.aliases._
import utils.db._

object LineItemManager {
  def getCartLineItems(refNum: String)(implicit ec: EC,
                                       db: DB): DbResultT[Seq[CartLineItemProductData]] =
    for {
      lineItems    ← * <~ CartLineItems.filter(_.cordRef === refNum).result
      lineItemData ← * <~ lineItems.map(getCartLineItem)
    } yield lineItemData

  def getOrderLineItems(refNum: String)(implicit ec: EC,
                                        db: DB): DbResultT[Seq[OrderLineItemProductData]] =
    for {
      lineItems    ← * <~ OrderLineItems.filter(_.cordRef === refNum).result
      lineItemData ← * <~ lineItems.map(getOrderLineItem)
    } yield lineItemData

  private def getCartLineItem(cartLineItem: CartLineItem)(implicit ec: EC, db: DB) =
    for {
      sku     ← * <~ SkuManager.mustFindFullSkuById(cartLineItem.skuId)
      product ← * <~ getProductForSku(sku.model)
    } yield
      CartLineItemProductData(sku = sku.model,
                              skuForm = sku.form,
                              skuShadow = sku.shadow,
                              productForm = product.form,
                              productShadow = product.shadow,
                              lineItem = cartLineItem)

  private def getOrderLineItem(orderLineItem: OrderLineItem)(implicit ec: EC, db: DB) =
    for {
      sku ← * <~ SkuManager.mustFindFullSkuByIdAndShadowId(orderLineItem.skuId,
                                                           orderLineItem.skuShadowId)
      product ← * <~ getProductForSku(sku.model)
    } yield
      OrderLineItemProductData(sku = sku.model,
                               skuForm = sku.form,
                               skuShadow = sku.shadow,
                               productForm = product.form,
                               productShadow = product.shadow,
                               lineItem = orderLineItem)

  private def getProductForSku(sku: Sku)(implicit ec: EC, db: DB) = {
    for {
      productId ← * <~ ProductSkuLinks.filter(_.rightId === sku.id).one.dbresult.flatMap {
                   case Some(productLink) ⇒
                     DbResultT.good(productLink.leftId)
                   case None ⇒
                     for {
                       valueLink ← * <~ VariantValueSkuLinks
                                    .filter(_.rightId === sku.id)
                                    .mustFindOneOr(NoProductFoundForSku(sku.id))
                       variantLink ← * <~ VariantValueLinks
                                      .filter(_.rightId === valueLink.leftId)
                                      .mustFindOneOr(NoProductFoundForSku(sku.id))
                       productLink ← * <~ ProductVariantLinks
                                      .filter(_.rightId === variantLink.leftId)
                                      .mustFindOneOr(NoProductFoundForSku(sku.id))
                     } yield productLink.leftId
                 }
      product ← * <~ ProductManager.mustFindFullProductById(productId)
    } yield product
  }
}
