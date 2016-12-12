package services

import cats.data._
import cats.implicits._
import failures.ProductFailures.NoProductFoundForSku
import models.cord.lineitems._
import models.image.{AlbumImageLinks, Albums, Images}
import models.inventory.ProductVariant
import models.objects._
import models.product._
import org.json4s.JsonAST.JString
import services.image.ImageManager
import services.inventory.SkuManager
import services.objects.ObjectManager
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
      image   ← * <~ getLineItemImage(sku.model, product.model)
    } yield
      CartLineItemProductData(sku = sku.model,
                              skuForm = sku.form,
                              skuShadow = sku.shadow,
                              productForm = product.form,
                              productShadow = product.shadow,
                              image = image,
                              lineItem = cartLineItem,
                              attributes = cartLineItem.attributes)

  private def getOrderLineItem(orderLineItem: OrderLineItem)(implicit ec: EC, db: DB) =
    for {
      sku ← * <~ SkuManager.mustFindFullSkuByIdAndShadowId(orderLineItem.skuId,
                                                           orderLineItem.skuShadowId)
      product ← * <~ getProductForSku(sku.model)
      image   ← * <~ getLineItemImage(sku.model, product.model)
    } yield
      OrderLineItemProductData(sku = sku.model,
                               skuForm = sku.form,
                               skuShadow = sku.shadow,
                               productForm = product.form,
                               productShadow = product.shadow,
                               image = image,
                               lineItem = orderLineItem,
                               attributes = orderLineItem.attributes)

  private def getProductForSku(sku: ProductVariant)(implicit ec: EC, db: DB) =
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

  private def getLineItemImage(sku: ProductVariant, product: Product)(implicit ec: EC, db: DB) =
    for {
      image ← * <~ getLineItemAlbumId(sku, product).flatMap {
               case Some(albumId) ⇒
                 for {
                   album ← * <~ Albums.mustFindById404(albumId)
                   image ← * <~ ImageManager.getFirstImageForAlbum(album)
                 } yield image

               case None ⇒
                 DbResultT.none[String]
             }
    } yield image

  private def getLineItemAlbumId(sku: ProductVariant, product: Product)(implicit ec: EC, db: DB) =
    for {
      albumId ← * <~ SkuAlbumLinks.filterLeft(sku).one.dbresult.flatMap {
                 case Some(albumLink) ⇒
                   DbResultT.good(albumLink.rightId.some)
                 case None ⇒
                   for {
                     albumLink ← * <~ ProductAlbumLinks.filterLeft(product).one.dbresult
                   } yield albumLink.map(_.rightId)
               }
    } yield albumId
}
